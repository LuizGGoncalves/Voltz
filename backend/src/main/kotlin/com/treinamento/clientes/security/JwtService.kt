package com.treinamento.clientes.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    private val jwtSecretKey: SecretKeySpec,
    private val jwtProperties: JwtProperties
) {

    private val signer by lazy { MACSigner(jwtSecretKey) }

    companion object {
        const val ISSUER = "gestao-clientes-api"
    }

    fun gerarAccessToken(username: String, authorities: Collection<GrantedAuthority>): String {
        val now = Instant.now()
        val expiration = now.plusMillis(jwtProperties.accessExpirationMs)
        val roles = authorities.map { it.authority }

        val claims = JWTClaimsSet.Builder()
            .subject(username)
            .issuer(ISSUER)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiration))
            .claim("roles", roles)
            .build()

        val signed = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        signed.sign(signer)
        return signed.serialize()
    }

    fun gerarRefreshToken(): String = UUID.randomUUID().toString()

    fun getAccessExpirationMs(): Long = jwtProperties.accessExpirationMs

    fun getRefreshExpirationMs(): Long = jwtProperties.refreshExpirationMs
}
