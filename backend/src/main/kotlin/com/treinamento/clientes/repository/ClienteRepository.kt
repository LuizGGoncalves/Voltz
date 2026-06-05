package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.Cliente
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ClienteRepository : JpaRepository<Cliente, Long> {

    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.unidadesConsumidoras WHERE c.id = :id")
    fun findByIdWithUCs(id: Long): Cliente?

    fun findAllByAtivoTrue(pageable: Pageable): Page<Cliente>

    fun findAllByAtivoFalse(pageable: Pageable): Page<Cliente>

    @Query("SELECT COUNT(*) > 0 FROM cliente WHERE documento = :documento AND ativo = true", nativeQuery = true)
    fun existsByDocumentoAndAtivoTrue(documento: String): Boolean

    @Query("SELECT c FROM Cliente c WHERE c.ativo = true ORDER BY c.createdAt DESC")
    fun findUltimos20(pageable: Pageable): Page<Cliente>
}
