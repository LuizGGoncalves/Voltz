package com.treinamento.clientes.security

import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {

    fun currentUsername(): String =
        SecurityContextHolder.getContext().authentication?.name ?: "anonymous"
}
