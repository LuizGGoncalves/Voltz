package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.UnidadeConsumidora
import org.springframework.data.jpa.repository.JpaRepository

interface UnidadeConsumidoraRepository : JpaRepository<UnidadeConsumidora, Long> {

    fun existsByNumeroInstalacaoAndAtivoTrue(numeroInstalacao: String): Boolean

    fun findByClienteIdAndAtivoTrue(clienteId: Long): List<UnidadeConsumidora>
}
