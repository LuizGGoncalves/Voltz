package com.treinamento.clientes.service

import com.treinamento.clientes.domain.model.RefreshToken
import com.treinamento.clientes.repository.RefreshTokenRepository
import com.treinamento.clientes.security.JwtProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties
) {

    @Transactional
    fun criar(usuarioId: Long, rawToken: String): RefreshToken {
        val token = RefreshToken(
            usuarioId = usuarioId,
            tokenHash = hash(rawToken),
            expiraEm = Instant.now().plusMillis(jwtProperties.refreshExpirationMs)
        )
        return refreshTokenRepository.save(token)
    }

    @Transactional(readOnly = true)
    fun validar(rawToken: String): RefreshToken {
        val token = refreshTokenRepository.findByTokenHashAndRevogadoFalse(hash(rawToken))
            .orElseThrow { RuntimeException("Refresh token inválido ou revogado") }

        if (token.expiraEm.isBefore(Instant.now())) {
            throw RuntimeException("Refresh token expirado")
        }

        return token
    }

    @Transactional
    fun revogar(rawToken: String) {
        val token = refreshTokenRepository.findByTokenHashAndRevogadoFalse(hash(rawToken))
        token.ifPresent {
            it.revogado = true
            refreshTokenRepository.save(it)
        }
    }

    @Transactional
    fun revogarTodosPorUsuario(usuarioId: Long) {
        refreshTokenRepository.revogarTodosPorUsuario(usuarioId)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }
}
