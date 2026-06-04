package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByTokenHashAndRevogadoFalse(tokenHash: String): Optional<RefreshToken>

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revogado = true WHERE r.usuarioId = :usuarioId AND r.revogado = false")
    fun revogarTodosPorUsuario(usuarioId: Long)
}
