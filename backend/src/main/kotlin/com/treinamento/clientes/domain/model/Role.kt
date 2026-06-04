package com.treinamento.clientes.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "role")
class Role(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var nome: String = ""
)
