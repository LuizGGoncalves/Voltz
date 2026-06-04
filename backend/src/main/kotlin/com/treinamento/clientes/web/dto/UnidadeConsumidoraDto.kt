package com.treinamento.clientes.web.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UnidadeConsumidoraRequest(
    @field:NotBlank(message = "Nome da UC é obrigatório")
    @field:Size(max = 255)
    val nome: String,

    @field:NotBlank(message = "Número de instalação é obrigatório")
    @field:Size(max = 50)
    val numeroInstalacao: String,

    @field:Valid
    val endereco: EnderecoRequest
)

data class UnidadeConsumidoraResponse(
    val id: Long,
    val nome: String,
    val numeroInstalacao: String,
    val endereco: EnderecoResponse,
    val ativo: Boolean
)
