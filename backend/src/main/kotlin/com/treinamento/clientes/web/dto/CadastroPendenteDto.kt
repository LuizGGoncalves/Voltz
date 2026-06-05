package com.treinamento.clientes.web.dto

import java.time.Instant

data class CadastroPendenteResponse(
    val id: Long,
    val documento: String,
    val status: String,
    val motivo: String?,
    val tentativas: Int,
    val createdAt: Instant,
    val ultimaTentativa: Instant?,
    val clienteId: Long?
)

data class CadastroPendenteCreatedResponse(
    val cadastroPendenteId: Long,
    val status: String = "PENDENTE",
    val mensagem: String = "Cadastro em processamento; o endereço será validado automaticamente."
)
