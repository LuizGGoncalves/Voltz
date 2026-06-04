package com.treinamento.clientes.web.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

data class ClienteRequest(
    @field:NotBlank(message = "Nome é obrigatório")
    @field:Size(max = 255)
    val nome: String,

    @field:NotBlank(message = "Documento é obrigatório")
    val documento: String,

    @field:Valid
    val endereco: EnderecoRequest,

    @field:Valid
    @field:NotEmpty(message = "Pelo menos uma unidade consumidora é obrigatória")
    val unidadesConsumidoras: List<UnidadeConsumidoraRequest>
)

data class ClienteResponse(
    val id: Long,
    val nome: String,
    val documento: String,
    val tipoDocumento: String,
    val ativo: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val endereco: EnderecoResponse,
    val unidadesConsumidoras: List<UnidadeConsumidoraResponse>
)

data class ClienteResumoResponse(
    val id: Long,
    val nome: String,
    val documento: String,
    val tipoDocumento: String,
    val ativo: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
