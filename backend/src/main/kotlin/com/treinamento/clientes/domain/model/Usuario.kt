package com.treinamento.clientes.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "usuario")
class Usuario(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 150)
    var username: String = "",

    @Column(nullable = false)
    var senha: String = "",

    @Column(nullable = false)
    var ativo: Boolean = true,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_role",
        joinColumns = [JoinColumn(name = "usuario_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf()
)
