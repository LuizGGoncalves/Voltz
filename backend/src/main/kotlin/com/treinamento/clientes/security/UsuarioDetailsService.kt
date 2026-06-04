package com.treinamento.clientes.security

import com.treinamento.clientes.repository.UsuarioRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UsuarioDetailsService(
    private val usuarioRepository: UsuarioRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val usuario = usuarioRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado: $username") }

        val authorities = usuario.roles.map { SimpleGrantedAuthority("ROLE_${it.nome}") }

        return User.builder()
            .username(usuario.username)
            .password(usuario.senha)
            .authorities(authorities)
            .disabled(!usuario.ativo)
            .build()
    }
}
