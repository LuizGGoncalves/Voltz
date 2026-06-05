package com.treinamento.clientes.service

import com.treinamento.clientes.domain.event.AnaliseClienteMgEvent
import com.treinamento.clientes.domain.model.Cliente
import com.treinamento.clientes.domain.model.Endereco
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.domain.rules.UfRules
import com.treinamento.clientes.exception.*
import com.treinamento.clientes.security.SecurityUtils
import com.treinamento.clientes.integration.viacep.ViaCepService
import com.treinamento.clientes.domain.model.AuditoriaDocumento
import com.treinamento.clientes.repository.AuditoriaDocumentoRepository
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.repository.UnidadeConsumidoraRepository
import com.treinamento.clientes.web.dto.ClienteRequest
import com.treinamento.clientes.web.dto.ClienteUpdateRequest
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
    private val eventPublisher: ApplicationEventPublisher,
    private val auditoriaRepository: AuditoriaDocumentoRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)


    @Transactional
    fun criar(request: ClienteRequest): Cliente {
        val documento = validarDocumento(request.documento)
        validarDocumentoUnico(documento.valor)
        validarInstalacoesUnicas(request.unidadesConsumidoras.map { it.numeroInstalacao })

        val cliente = request.toModel()

        // Enriquecer endereços via ViaCEP
        val enderecoCliente = viaCepService.consultar(request.endereco.cep)
        cliente.endereco.enriquecerCom(enderecoCliente)

        cliente.unidadesConsumidoras.forEachIndexed { i, uc ->
            val enderecoUc = viaCepService.consultar(request.unidadesConsumidoras[i].endereco.cep)
            uc.endereco.enriquecerCom(enderecoUc)
        }

        val salvo = finalizarCadastro(cliente)
        log.info("Cliente criado: id={}, user={}", salvo.id, SecurityUtils.currentUsername())
        return salvo
    }

    /**
     * FONTE ÚNICA das regras de UF.
     * Chamado pelo caminho síncrono (criar) e pelo job de retry (Sprint 4).
     */
    fun finalizarCadastro(cliente: Cliente): Cliente {
        // Regra de UF nas UCs
        cliente.unidadesConsumidoras.forEach { uc ->
            val uf = uc.endereco.uf.uppercase()
            if (uf in UfRules.BLOQUEADAS) {
                throw UfBloqueadaException(uf, uc.nome)
            }
        }

        val salvo = clienteRepository.save(cliente)

        // Evento MG — publicado APÓS o save (listener roda after commit)
        salvo.unidadesConsumidoras.forEach { uc ->
            if (uc.endereco.uf.uppercase() == UfRules.EVENTO_MG) {
                log.info("UC '{}' em MG — publicando evento analise_cliente_mg", uc.nome)
                eventPublisher.publishEvent(
                    AnaliseClienteMgEvent(
                        clienteId = requireNotNull(salvo.id),
                        unidadeConsumidoraId = requireNotNull(uc.id)
                    )
                )
            }
        }

        return salvo
    }

    @Transactional
    fun atualizar(id: Long, request: ClienteUpdateRequest): Cliente {
        val cliente = clienteRepository.findByIdWithUCs(id)
            ?: throw ClienteNaoEncontradoException(id)
        val documento = validarDocumento(request.documento)

        if (cliente.documento?.valor != documento.valor) {
            validarDocumentoUnico(documento.valor)
        }

        cliente.nome = request.nome
        cliente.documento = documento

        val enderecoCliente = viaCepService.consultar(request.endereco.cep)
        cliente.endereco = request.endereco.toModel()
        cliente.endereco.enriquecerCom(enderecoCliente)

        val salvo = clienteRepository.save(cliente)
        log.info("Cliente atualizado: id={}, user={}", salvo.id, SecurityUtils.currentUsername())
        return salvo
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Cliente =
        clienteRepository.findByIdWithUCs(id) ?: throw ClienteNaoEncontradoException(id)

    @Transactional(readOnly = true)
    fun listar(pageable: Pageable, incluirInativos: Boolean = false): Page<Cliente> =
        if (incluirInativos) clienteRepository.findAll(pageable)
        else clienteRepository.findAllByAtivoTrue(pageable)

    @Transactional(readOnly = true)
    fun ultimos20(): Page<Cliente> =
        clienteRepository.findUltimos20(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))

    @Transactional
    fun corrigirDocumento(id: Long, novoDocumento: String, motivo: String): Cliente {
        val cliente = clienteRepository.findByIdWithUCs(id)
            ?: throw ClienteNaoEncontradoException(id)
        val documento = validarDocumento(novoDocumento)
        val docAnterior = cliente.documento?.valor ?: ""

        if (docAnterior != documento.valor) {
            validarDocumentoUnico(documento.valor)
        }

        cliente.documento = documento
        val salvo = clienteRepository.save(cliente)

        // Auditoria em tabela (M5)
        auditoriaRepository.save(
            AuditoriaDocumento(
                clienteId = id,
                documentoAnterior = docAnterior,
                documentoNovo = documento.valor,
                motivo = motivo,
                usuario = SecurityUtils.currentUsername()
            )
        )

        log.info("Documento corrigido: clienteId={}, de={} para={}, motivo='{}', user={}",
            id, docAnterior, documento.valor, motivo, SecurityUtils.currentUsername())
        return salvo
    }

    @Transactional
    fun inativar(id: Long) {
        val cliente = buscarOuFalhar(id)
        log.info("Cliente inativado: id={}, nome={}, user={}", id, cliente.nome, SecurityUtils.currentUsername())
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

}
