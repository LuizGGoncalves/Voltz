package com.treinamento.clientes.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtKeyConfig(private val jwtProperties: JwtProperties) {

    @Bean
    fun jwtSecretKey(): SecretKeySpec {
        val keyBytes = jwtProperties.secret.toByteArray()
        require(keyBytes.size >= 32) {
            "JWT secret deve ter pelo menos 32 bytes (256 bits). Atual: ${keyBytes.size} bytes. " +
                "Configure JWT_SECRET no .env com um valor forte."
        }
        return SecretKeySpec(keyBytes, "HmacSHA256")
    }
}
