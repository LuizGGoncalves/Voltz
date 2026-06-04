package com.treinamento.clientes.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessExpirationMs: Long = 900_000,
    val refreshExpirationMs: Long = 604_800_000
)
