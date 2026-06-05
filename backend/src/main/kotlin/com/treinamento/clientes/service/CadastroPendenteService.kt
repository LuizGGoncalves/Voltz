package com.treinamento.clientes.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.treinamento.clientes.domain.model.CadastroPendente
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.exception.ClienteNaoEncontradoException
import com.treinamento.clientes.exception.DocumentoDuplicadoException
import com.treinamento.clientes.exception.InstalacaoDuplicadaException
import com.treinamento.clientes.repository.CadastroPendenteRepository
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.repository.UnidadeConsumidoraRepository
import com.treinamento.clientes.web.dto.CadastroPendenteResponse
import com.treinamento.clientes.web.dto.ClienteRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CadastroPendenteService(
    private val cadastroPendenteRepository: CadastroPendenteRepository,
    private val clienteRepository: ClienteRepository,
    private val ucRepository: UnidadeConsumidoraRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun enfileirar(request: ClienteRequest): CadastroPendente {
        val docNormalizado = Documento.of(request.documento).valor

        if (clienteRepository.existsByDocumentoAndAtivoTrue(docNormalizado)) {
            throw DocumentoDuplicadoException(docNormalizado)
        }

        request.unidadesConsumidoras.forEach { ucReq ->
            if (ucRepository.existsByNumeroInstalacaoAndAtivoTrue(ucReq.numeroInstalacao)) {
                throw InstalacaoDuplicadaException(ucReq.numeroInstalacao)
            }
        }

        val payload = objectMapper.writeValueAsString(request)
        val inserted = cadastroPendenteRepository.insertOnConflictDoNothing(docNormalizado, payload)

        if (inserted == 0) {
            throw DocumentoDuplicadoException(docNormalizado)
        }

        return cadastroPendenteRepository.findByDocumentoAndStatus(docNormalizado, "PENDENTE")!!
    }

    @Transactional(readOnly = true)
    fun listar(pageable: Pageable, status: String?): Page<CadastroPendenteResponse> {
        val page = if (status != null) {
            cadastroPendenteRepository.findAllByStatusOrderByCreatedAtDesc(status.uppercase(), pageable)
        } else {
            cadastroPendenteRepository.findAllByOrderByCreatedAtDesc(pageable)
        }
        return page.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): CadastroPendenteResponse {
        val pendente = cadastroPendenteRepository.findById(id)
            .orElseThrow { ClienteNaoEncontradoException(id) }
        return pendente.toResponse()
    }

    private fun CadastroPendente.toResponse() = CadastroPendenteResponse(
        id = id!!,
        documento = documento,
        status = status,
        motivo = motivo,
        tentativas = tentativas,
        createdAt = createdAt,
        ultimaTentativa = ultimaTentativa
    )
}
