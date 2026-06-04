package com.treinamento.clientes.service

import com.treinamento.clientes.domain.event.AnaliseClienteMgEvent
import com.treinamento.clientes.domain.model.Cliente
import com.treinamento.clientes.domain.model.Endereco
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.exception.*
import com.treinamento.clientes.integration.viacep.ViaCepService
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.repository.UnidadeConsumidoraRepository
import com.treinamento.clientes.web.dto.ClienteRequest
import com.treinamento.clientes.web.mapper.toModel
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClienteService(
    private val clienteRepository: ClienteRepository,
    private val ucRepository: UnidadeConsumidoraRepository,
    private val viaCepService: ViaCepService,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val UFS_BLOQUEADAS = setOf("SP", "RS", "PR")
        const val UF_EVENTO_MG = "MG"
    }

    @Transactional
    fun criar(request: ClienteRequest): Cliente {
        val documento = validarDocumento(request.documento)
        validarDocumentoUnico(documento.valor)
        validarInstalacoesUnicas(request.unidadesConsumidoras.map { it.numeroInstalacao })

        val cliente = request.toModel()

        // Enriquecer endereços via ViaCEP
        val enderecoCliente = viaCepService.consultar(request.endereco.cep)
        enriquecerEndereco(cliente.endereco, enderecoCliente)

        cliente.unidadesConsumidoras.forEachIndexed { i, uc ->
            val enderecoUc = viaCepService.consultar(request.unidadesConsumidoras[i].endereco.cep)
            enriquecerEndereco(uc.endereco, enderecoUc)
        }

        return finalizarCadastro(cliente)
    }

    /**
     * FONTE ÚNICA das regras de UF.
     * Chamado pelo caminho síncrono (criar) e pelo job de retry (Sprint 4).
     */
    fun finalizarCadastro(cliente: Cliente): Cliente {
        // Regra de UF nas UCs
        cliente.unidadesConsumidoras.forEach { uc ->
            val uf = uc.endereco.uf.uppercase()
            if (uf in UFS_BLOQUEADAS) {
                throw UfBloqueadaException(uf, uc.nome)
            }
        }

        val salvo = clienteRepository.save(cliente)

        // Evento MG — publicado APÓS o save (listener roda after commit)
        salvo.unidadesConsumidoras.forEach { uc ->
            if (uc.endereco.uf.uppercase() == UF_EVENTO_MG) {
                log.info("UC '{}' em MG — publicando evento analise_cliente_mg", uc.nome)
                eventPublisher.publishEvent(
                    AnaliseClienteMgEvent(
                        clienteId = salvo.id!!,
                        unidadeConsumidoraId = uc.id!!
                    )
                )
            }
        }

        return salvo
    }

    @Transactional
    fun atualizar(id: Long, request: ClienteRequest): Cliente {
        val cliente = buscarOuFalhar(id)
        val documento = validarDocumento(request.documento)

        if (cliente.documento?.valor != documento.valor) {
            validarDocumentoUnico(documento.valor)
        }

        val instalacoesProprias = cliente.unidadesConsumidoras
            .filter { it.ativo }
            .map { it.numeroInstalacao }
            .toSet()
        val novasInstalacoes = request.unidadesConsumidoras.map { it.numeroInstalacao }
        val instalacoesExternas = novasInstalacoes.filterNot { it in instalacoesProprias }
        validarInstalacoesUnicas(instalacoesExternas)

        cliente.nome = request.nome
        cliente.documento = documento
        cliente.endereco = request.endereco.toModel()

        // Enriquecer endereço do cliente
        val enderecoCliente = viaCepService.consultar(request.endereco.cep)
        enriquecerEndereco(cliente.endereco, enderecoCliente)

        // Sincronizar UCs
        cliente.unidadesConsumidoras.clear()
        request.unidadesConsumidoras.forEachIndexed { i, ucReq ->
            val uc = ucReq.toModel()
            val enderecoUc = viaCepService.consultar(ucReq.endereco.cep)
            enriquecerEndereco(uc.endereco, enderecoUc)
            cliente.adicionarUC(uc)
        }

        return finalizarCadastro(cliente)
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Cliente =
        clienteRepository.findByIdWithUCs(id).orElseThrow { ClienteNaoEncontradoException(id) }

    @Transactional(readOnly = true)
    fun listar(pageable: Pageable, incluirInativos: Boolean = false): Page<Cliente> =
        if (incluirInativos) clienteRepository.findAll(pageable)
        else clienteRepository.findAllByAtivoTrue(pageable)

    @Transactional(readOnly = true)
    fun ultimos20(): Page<Cliente> =
        clienteRepository.findUltimos20(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))

    @Transactional
    fun corrigirDocumento(id: Long, novoDocumento: String): Cliente {
        val cliente = clienteRepository.findByIdWithUCs(id)
            .orElseThrow { ClienteNaoEncontradoException(id) }
        val documento = validarDocumento(novoDocumento)
        if (cliente.documento?.valor != documento.valor) {
            validarDocumentoUnico(documento.valor)
        }
        cliente.documento = documento
        return clienteRepository.save(cliente)
    }

    @Transactional
    fun inativar(id: Long) {
        val cliente = buscarOuFalhar(id)
        clienteRepository.delete(cliente)
    }

    private fun buscarOuFalhar(id: Long): Cliente =
        clienteRepository.findById(id).orElseThrow { ClienteNaoEncontradoException(id) }

    private fun validarDocumento(doc: String): Documento {
        if (!Documento.ehValido(doc)) throw DocumentoInvalidoException(doc)
        return Documento.of(doc)
    }

    private fun validarDocumentoUnico(documento: String) {
        if (clienteRepository.existsByDocumentoAndAtivoTrue(documento)) {
            throw DocumentoDuplicadoException(documento)
        }
    }

    private fun validarInstalacoesUnicas(instalacoes: List<String>) {
        instalacoes.forEach { numero ->
            if (ucRepository.existsByNumeroInstalacaoAndAtivoTrue(numero)) {
                throw InstalacaoDuplicadaException(numero)
            }
        }
    }

    private fun enriquecerEndereco(destino: Endereco, viaCep: Endereco) {
        destino.logradouro = viaCep.logradouro
        destino.bairro = viaCep.bairro
        destino.cidade = viaCep.cidade
        destino.uf = viaCep.uf
    }
}
