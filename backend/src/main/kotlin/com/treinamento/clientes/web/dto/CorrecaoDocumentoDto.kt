package com.treinamento.clientes.web.dto

import jakarta.validation.constraints.NotBlank

data class CorrecaoDocumentoRequest(
    @field:NotBlank(message = "Documento é obrigatório")
    val documento: String,

    @field:NotBlank(message = "Motivo é obrigatório")
    val motivo: String
)
