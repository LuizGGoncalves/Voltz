package com.treinamento.clientes.web.controller

import com.treinamento.clientes.repository.UsuarioRepository
import com.treinamento.clientes.security.JwtService
import com.treinamento.clientes.service.RefreshTokenService
import com.treinamento.clientes.web.dto.LoginRequest
import com.treinamento.clientes.web.dto.LoginResponse
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val usuarioRepository: UsuarioRepository
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginResponse> {
        val authentication: Authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        val accessToken = jwtService.gerarAccessToken(authentication.name, authentication.authorities)
        val rawRefreshToken = jwtService.gerarRefreshToken()

        val usuario = usuarioRepository.findByUsername(authentication.name).get()
        refreshTokenService.criar(usuario.id!!, rawRefreshToken)

        val cookie = buildRefreshCookie(rawRefreshToken, jwtService.getRefreshExpirationMs() / 1000)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                expiresIn = jwtService.getAccessExpirationMs() / 1000
            )
        )
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<LoginResponse> {
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val token = refreshTokenService.validar(refreshToken)
        val usuario = usuarioRepository.findById(token.usuarioId).get()

        // Rotacionar: revogar o antigo e emitir novo
        refreshTokenService.revogar(refreshToken)

        val authorities = usuario.roles.map {
            org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_${it.nome}")
        }
        val accessToken = jwtService.gerarAccessToken(usuario.username, authorities)
        val newRawRefreshToken = jwtService.gerarRefreshToken()
        refreshTokenService.criar(usuario.id!!, newRawRefreshToken)

        val cookie = buildRefreshCookie(newRawRefreshToken, jwtService.getRefreshExpirationMs() / 1000)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                expiresIn = jwtService.getAccessExpirationMs() / 1000
            )
        )
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue("refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        if (!refreshToken.isNullOrBlank()) {
            refreshTokenService.revogar(refreshToken)
        }

        val cookie = buildRefreshCookie("", 0)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.noContent().build()
    }

    private fun buildRefreshCookie(value: String, maxAge: Long): ResponseCookie =
        ResponseCookie.from("refresh_token", value)
            .httpOnly(true)
            .secure(false) // dev: sem HTTPS; em prod: true
            .path("/api/v1/auth")
            .maxAge(maxAge)
            .sameSite("Lax")
            .build()
}
