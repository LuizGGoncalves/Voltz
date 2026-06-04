package com.treinamento.clientes.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "analise_cliente_mg")
class AnaliseClienteMg(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "cliente_id", nullable = false)
    var clienteId: Long = 0,

    @Column(name = "unidade_consumidora_id", nullable = false)
    var unidadeConsumidoraId: Long = 0,

    @Column(nullable = false, length = 30)
    var status: String = "PENDENTE_ANALISE",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
