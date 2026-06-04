package com.treinamento.clientes.service

import com.treinamento.clientes.domain.model.Cliente
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.exception.ClienteNaoEncontradoException
import com.treinamento.clientes.exception.DocumentoDuplicadoException
import com.treinamento.clientes.exception.DocumentoInvalidoException
import com.treinamento.clientes.exception.InstalacaoDuplicadaException
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.repository.UnidadeConsumidoraRepository
import com.treinamento.clientes.web.dto.ClienteRequest
import com.treinamento.clientes.web.mapper.toModel
import com.treinamento.clientes.web.mapper.toResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClienteService(
    private val clienteRepository: ClienteRepository,
    private val ucRepository: UnidadeConsumidoraRepository
) {

    @Transactional
    fun criar(request: ClienteRequest): Cliente {
        val documento = validarDocumento(request.documento)
        validarDocumentoUnico(documento.valor)
        validarInstalacoesUnicas(request.unidadesConsumidoras.map { it.numeroInstalacao })

        val cliente = request.toModel()
        return clienteRepository.save(cliente)
    }

    @Transactional
    fun atualizar(id: Long, request: ClienteRequest): Cliente {
        val cliente = buscarOuFalhar(id)
        val documento = validarDocumento(request.documento)

        // Se o documento mudou, valida unicidade
        if (cliente.documento.valor != documento.valor) {
            validarDocumentoUnico(documento.valor)
        }

        // Valida instalações (excluindo as do próprio cliente)
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

        // Sincronizar UCs: limpar e re-adicionar
        cliente.unidadesConsumidoras.clear()
        request.unidadesConsumidoras.forEach { ucReq ->
            cliente.adicionarUC(ucReq.toModel())
        }

        return clienteRepository.save(cliente)
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Cliente = buscarOuFalhar(id)

    @Transactional(readOnly = true)
    fun listar(pageable: Pageable, incluirInativos: Boolean = false): Page<Cliente> =
        if (incluirInativos) clienteRepository.findAll(pageable)
        else clienteRepository.findAllByAtivoTrue(pageable)

    @Transactional(readOnly = true)
    fun ultimos20(): Page<Cliente> =
        clienteRepository.findUltimos20(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))

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
}
