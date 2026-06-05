package com.treinamento.clientes.domain.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "cadastro_pendente")
class CadastroPendente(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 14)
    var documento: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSONB")
    var payload: String = "",

    @Column(nullable = false, length = 20)
    var status: String = "PENDENTE",

    @Column(length = 500)
    var motivo: String? = null,

    @Column(nullable = false)
    var tentativas: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "ultima_tentativa")
    var ultimaTentativa: Instant? = null
)
