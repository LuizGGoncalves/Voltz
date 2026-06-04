package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.Cliente
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ClienteRepository : JpaRepository<Cliente, Long> {

    fun findAllByAtivoTrue(pageable: Pageable): Page<Cliente>

    fun findAllByAtivoFalse(pageable: Pageable): Page<Cliente>

    fun existsByDocumentoAndAtivoTrue(documento: String): Boolean

    @Query("SELECT c FROM Cliente c WHERE c.ativo = true ORDER BY c.createdAt DESC")
    fun findUltimos20(pageable: Pageable): Page<Cliente>
}
