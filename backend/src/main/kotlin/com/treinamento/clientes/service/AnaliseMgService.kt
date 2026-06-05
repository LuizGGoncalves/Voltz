package com.treinamento.clientes.service

import com.treinamento.clientes.repository.AnaliseClienteMgRepository
import com.treinamento.clientes.web.dto.AnaliseClienteMgResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnaliseMgService(
    private val repository: AnaliseClienteMgRepository
) {

    @Transactional(readOnly = true)
    fun listar(pageable: Pageable): Page<AnaliseClienteMgResponse> =
        repository.findAllByOrderByCreatedAtDesc(pageable).map {
            AnaliseClienteMgResponse(
                id = requireNotNull(it.id),
                clienteId = it.clienteId,
                unidadeConsumidoraId = it.unidadeConsumidoraId,
                status = it.status,
                createdAt = it.createdAt
            )
        }
}
