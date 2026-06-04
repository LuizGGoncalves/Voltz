package com.treinamento.clientes.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Endereco(
    @Column(nullable = false, length = 8)
    var cep: String = "",

    @Column(nullable = false)
    var logradouro: String = "",

    @Column(nullable = false, length = 20)
    var numero: String = "",

    @Column(length = 100)
    var complemento: String? = null,

    @Column(nullable = false, length = 100)
    var bairro: String = "",

    @Column(nullable = false, length = 100)
    var cidade: String = "",

    @Column(nullable = false, length = 2)
    var uf: String = ""
)
