package com.treinamento.clientes.domain.event

data class AnaliseClienteMgEvent(
    val clienteId: Long,
    val unidadeConsumidoraId: Long
)
