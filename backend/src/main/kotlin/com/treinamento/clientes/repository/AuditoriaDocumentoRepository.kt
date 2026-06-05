package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.AuditoriaDocumento
import org.springframework.data.jpa.repository.JpaRepository

interface AuditoriaDocumentoRepository : JpaRepository<AuditoriaDocumento, Long> {

    fun findByClienteIdOrderByCreatedAtDesc(clienteId: Long): List<AuditoriaDocumento>
}
