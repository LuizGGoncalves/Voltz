package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.AnaliseClienteMg
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AnaliseClienteMgRepository : JpaRepository<AnaliseClienteMg, Long> {

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AnaliseClienteMg>
}
