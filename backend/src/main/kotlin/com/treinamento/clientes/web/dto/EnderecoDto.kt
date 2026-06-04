package com.treinamento.clientes.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class EnderecoRequest(
    @field:NotBlank(message = "CEP é obrigatório")
    @field:Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido")
    val cep: String,

    @field:NotBlank(message = "Número é obrigatório")
    @field:Size(max = 20)
    val numero: String,

    @field:Size(max = 100)
    val complemento: String? = null
)

data class EnderecoResponse(
    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String?,
    val bairro: String,
    val cidade: String,
    val uf: String
)
