package com.treinamento.clientes.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
        val authorities = roles.map { SimpleGrantedAuthority(it) }
        return JwtAuthenticationToken(jwt, authorities, jwt.subject)
    }
}
