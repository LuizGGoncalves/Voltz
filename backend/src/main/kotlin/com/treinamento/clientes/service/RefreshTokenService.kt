package com.treinamento.clientes.service

import com.treinamento.clientes.domain.model.RefreshToken
import com.treinamento.clientes.repository.RefreshTokenRepository
import com.treinamento.clientes.security.JwtProperties
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties
) {
    private val hmacKey: SecretKeySpec by lazy {
        SecretKeySpec(jwtProperties.secret.toByteArray(), "HmacSHA256")
    }

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
            ?: throw UsernameNotFoundException("Refresh token inválido ou revogado")

        if (token.expiraEm.isBefore(Instant.now())) {
            throw UsernameNotFoundException("Refresh token expirado")
        }

        return token
    }

    @Transactional
    fun revogar(rawToken: String) {
        val token = refreshTokenRepository.findByTokenHashAndRevogadoFalse(hash(rawToken))
        if (token != null) {
            token.revogado = true
            refreshTokenRepository.save(token)
        }
    }

    @Transactional
    fun revogarTodosPorUsuario(usuarioId: Long) {
        refreshTokenRepository.revogarTodosPorUsuario(usuarioId)
    }

    private fun hash(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        val bytes = mac.doFinal(value.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }
}
