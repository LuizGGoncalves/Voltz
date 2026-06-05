package com.treinamento.clientes.web.controller

import com.treinamento.clientes.exception.LimiteTentativasExcedidoException
import com.treinamento.clientes.repository.UsuarioRepository
import com.treinamento.clientes.security.JwtService
import com.treinamento.clientes.security.RateLimitService
import com.treinamento.clientes.service.RefreshTokenService
import org.slf4j.LoggerFactory
import com.treinamento.clientes.web.dto.LoginRequest
import com.treinamento.clientes.web.dto.LoginResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService,
    private val usuarioRepository: UsuarioRepository,
    private val rateLimitService: RateLimitService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequest,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginResponse> {
        val clientIp = httpRequest.remoteAddr
        if (!rateLimitService.tryConsume(clientIp)) {
            log.warn("Rate limit excedido para IP={}", clientIp)
            throw LimiteTentativasExcedidoException()
        }

        val authentication: Authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(loginRequest.username, loginRequest.password)
            )
        } catch (ex: AuthenticationException) {
            log.warn("Login falhou: username={}, ip={}, motivo={}", loginRequest.username, clientIp, ex.message)
            throw ex
        }

        log.info("Login bem-sucedido: username={}, ip={}", authentication.name, clientIp)
        val accessToken = jwtService.gerarAccessToken(authentication.name, authentication.authorities)
        val rawRefreshToken = jwtService.gerarRefreshToken()

        val usuario = usuarioRepository.findByUsername(authentication.name)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado: ${authentication.name}") }
        refreshTokenService.criar(usuario.id!!, rawRefreshToken)

        val cookie = buildRefreshCookie(rawRefreshToken, jwtService.getRefreshExpirationMs() / 1000, httpRequest.isSecure)
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
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginResponse> {
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val token = refreshTokenService.validar(refreshToken)
        val usuario = usuarioRepository.findById(token.usuarioId)
            .orElseThrow { UsernameNotFoundException("Usuário não encontrado: id=${token.usuarioId}") }

        log.info("Refresh token rotacionado: username={}", usuario.username)
        refreshTokenService.revogar(refreshToken)

        val authorities = usuario.roles.map { SimpleGrantedAuthority("ROLE_${it.nome}") }
        val accessToken = jwtService.gerarAccessToken(usuario.username, authorities)
        val newRawRefreshToken = jwtService.gerarRefreshToken()
        refreshTokenService.criar(usuario.id!!, newRawRefreshToken)

        val cookie = buildRefreshCookie(newRawRefreshToken, jwtService.getRefreshExpirationMs() / 1000, httpRequest.isSecure)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                expiresIn = jwtService.getAccessExpirationMs() / 1000
            )
        )
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    fun logout(
        @CookieValue("refresh_token", required = false) refreshToken: String?,
        httpRequest: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        log.info("Logout: ip={}", httpRequest.remoteAddr)
        if (!refreshToken.isNullOrBlank()) {
            refreshTokenService.revogar(refreshToken)
        }

        val cookie = buildRefreshCookie("", 0, httpRequest.isSecure)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.noContent().build()
    }

    private fun buildRefreshCookie(value: String, maxAge: Long, secure: Boolean): ResponseCookie =
        ResponseCookie.from("refresh_token", value)
            .httpOnly(true)
            .secure(secure) // Derivado de request.isSecure — acompanha o protocolo automaticamente
            .path("/api/v1/auth")
            .maxAge(maxAge)
            .sameSite("Lax")
            .build()
}
