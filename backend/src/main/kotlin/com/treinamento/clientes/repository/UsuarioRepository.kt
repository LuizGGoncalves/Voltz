package com.treinamento.clientes.repository

import com.treinamento.clientes.domain.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UsuarioRepository : JpaRepository<Usuario, Long> {

    fun findByUsername(username: String): Optional<Usuario>
}
