package com.treinamento.clientes.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "auditoria_documento")
class AuditoriaDocumento(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "cliente_id", nullable = false)
    var clienteId: Long = 0,

    @Column(name = "documento_anterior", nullable = false, length = 14)
    var documentoAnterior: String = "",

    @Column(name = "documento_novo", nullable = false, length = 14)
    var documentoNovo: String = "",

    @Column(nullable = false, length = 500)
    var motivo: String = "",

    @Column(nullable = false, length = 150)
    var usuario: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
