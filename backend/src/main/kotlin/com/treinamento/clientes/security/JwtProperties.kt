package com.treinamento.clientes.security

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,

    @field:Min(60_000, message = "Access token mínimo: 1 minuto")
    @field:Max(3_600_000, message = "Access token máximo: 1 hora")
    val accessExpirationMs: Long = 900_000,

    @field:Min(3_600_000, message = "Refresh token mínimo: 1 hora")
    @field:Max(2_592_000_000, message = "Refresh token máximo: 30 dias")
    val refreshExpirationMs: Long = 604_800_000
)
