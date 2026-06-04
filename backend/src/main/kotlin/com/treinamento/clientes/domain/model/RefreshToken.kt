package com.treinamento.clientes.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "refresh_token")
class RefreshToken(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "usuario_id", nullable = false)
    var usuarioId: Long = 0,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String = "",

    @Column(name = "expira_em", nullable = false)
    var expiraEm: Instant = Instant.now(),

    @Column(nullable = false)
    var revogado: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
