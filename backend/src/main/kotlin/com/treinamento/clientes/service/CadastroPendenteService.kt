package com.treinamento.clientes.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.treinamento.clientes.domain.model.CadastroPendente
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.exception.DocumentoDuplicadoException
import com.treinamento.clientes.repository.CadastroPendenteRepository
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.web.dto.ClienteRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CadastroPendenteService(
    private val cadastroPendenteRepository: CadastroPendenteRepository,
    private val clienteRepository: ClienteRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun enfileirar(request: ClienteRequest): CadastroPendente {
        val docNormalizado = Documento.of(request.documento).valor

        // Dedup: checar contra clientes ativos E fila PENDENTE
        if (clienteRepository.existsByDocumentoAndAtivoTrue(docNormalizado)) {
            throw DocumentoDuplicadoException(docNormalizado)
        }
        if (cadastroPendenteRepository.existsPendenteByDocumento(docNormalizado)) {
            throw DocumentoDuplicadoException(docNormalizado)
        }

        val pendente = CadastroPendente(
            documento = docNormalizado,
            payload = objectMapper.writeValueAsString(request)
        )
        return cadastroPendenteRepository.save(pendente)
    }
}
