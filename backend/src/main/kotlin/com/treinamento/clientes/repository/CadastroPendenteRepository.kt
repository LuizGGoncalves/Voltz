package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.CadastroPendente
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CadastroPendenteRepository : JpaRepository<CadastroPendente, Long> {

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<CadastroPendente>

    fun findAllByStatusOrderByCreatedAtDesc(status: String, pageable: Pageable): Page<CadastroPendente>

    @Query("SELECT cp FROM CadastroPendente cp WHERE cp.status = 'PENDENTE' ORDER BY cp.createdAt ASC")
    fun findPendentesParaRetry(): List<CadastroPendente>

    @Query("SELECT COUNT(*) > 0 FROM cadastro_pendente WHERE documento = :documento AND status = 'PENDENTE'", nativeQuery = true)
    fun existsPendenteByDocumento(documento: String): Boolean
}
