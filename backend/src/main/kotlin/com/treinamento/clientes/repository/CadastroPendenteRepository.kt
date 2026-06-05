package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.CadastroPendente
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CadastroPendenteRepository : JpaRepository<CadastroPendente, Long> {

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<CadastroPendente>

    fun findAllByStatusOrderByCreatedAtDesc(status: String, pageable: Pageable): Page<CadastroPendente>

    @Query("SELECT cp FROM CadastroPendente cp WHERE cp.status = 'PENDENTE' ORDER BY cp.createdAt ASC LIMIT :limit")
    fun findPendentesParaRetry(@Param("limit") limit: Int = 100): List<CadastroPendente>

    @Query("SELECT COUNT(*) > 0 FROM cadastro_pendente WHERE documento = :documento AND status = 'PENDENTE'", nativeQuery = true)
    fun existsPendenteByDocumento(@Param("documento") documento: String): Boolean

    fun findByDocumentoAndStatus(documento: String, status: String): CadastroPendente?

    @Modifying
    @Query("""
        INSERT INTO cadastro_pendente (documento, payload, status, tentativas, created_at)
        VALUES (:documento, CAST(:payload AS JSONB), 'PENDENTE', 0, now())
        ON CONFLICT (documento) WHERE status = 'PENDENTE'
        DO NOTHING
    """, nativeQuery = true)
    fun insertOnConflictDoNothing(
        @Param("documento") documento: String,
        @Param("payload") payload: String
    ): Int
}
