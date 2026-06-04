package com.treinamento.clientes.web.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Username é obrigatório")
    val username: String,

    @field:NotBlank(message = "Senha é obrigatória")
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)
