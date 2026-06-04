package com.treinamento.clientes.web.dto

import java.time.Instant

data class AnaliseClienteMgResponse(
    val id: Long,
    val clienteId: Long,
    val unidadeConsumidoraId: Long,
    val status: String,
    val createdAt: Instant
)
