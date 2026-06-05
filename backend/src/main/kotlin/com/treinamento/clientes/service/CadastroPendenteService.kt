package com.treinamento.clientes.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.treinamento.clientes.domain.model.CadastroPendente
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.exception.DocumentoDuplicadoException
import com.treinamento.clientes.exception.InstalacaoDuplicadaException
import com.treinamento.clientes.repository.CadastroPendenteRepository
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.repository.UnidadeConsumidoraRepository
import com.treinamento.clientes.web.dto.ClienteRequest
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

        // Dedup documento: checar contra clientes ativos
        if (clienteRepository.existsByDocumentoAndAtivoTrue(docNormalizado)) {
            throw DocumentoDuplicadoException(docNormalizado)
        }

        // Dedup instalação: checar contra UCs ativas (risco #13 PLANO)
        request.unidadesConsumidoras.forEach { ucReq ->
            if (ucRepository.existsByNumeroInstalacaoAndAtivoTrue(ucReq.numeroInstalacao)) {
                throw InstalacaoDuplicadaException(ucReq.numeroInstalacao)
            }
        }

        // INSERT atômico com ON CONFLICT — resolve race condition (M1)
        val payload = objectMapper.writeValueAsString(request)
        val inserted = cadastroPendenteRepository.insertOnConflictDoNothing(docNormalizado, payload)

        if (inserted == 0) {
            throw DocumentoDuplicadoException(docNormalizado)
        }

        return cadastroPendenteRepository.findByDocumentoAndStatus(docNormalizado, "PENDENTE")!!
    }
}
