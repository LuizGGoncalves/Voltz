package com.treinamento.clientes.web.controller

import com.treinamento.clientes.exception.LimiteTentativasExcedidoException
import com.treinamento.clientes.repository.UsuarioRepository
import com.treinamento.clientes.security.JwtService
import com.treinamento.clientes.security.RateLimitService
import com.treinamento.clientes.service.RefreshTokenService
import org.slf4j.LoggerFactory
import com.treinamento.clientes.web.dto.LoginRequest
import com.treinamento.clientes.web.dto.LoginResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Autenticação", description = "Login, refresh token e logout")
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

    @Operation(
        summary = "Login",
        description = "Autentica com username/senha e retorna access token JWT + refresh token em cookie httpOnly. Rate limit: 5 tentativas / 15 min por IP.",
        responses = [
            ApiResponse(responseCode = "200", description = "Login bem-sucedido — access token no body, refresh token no cookie"),
            ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            ApiResponse(responseCode = "429", description = "Rate limit excedido (5 tentativas / 15 min)")
        ]
    )
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
        refreshTokenService.criar(requireNotNull(usuario.id), rawRefreshToken)

        val cookie = buildRefreshCookie(rawRefreshToken, jwtService.getRefreshExpirationMs() / 1000, httpRequest.isSecure)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                expiresIn = jwtService.getAccessExpirationMs() / 1000
            )
        )
    }

    @Operation(
        summary = "Renovar tokens",
        description = "Usa o refresh token (cookie httpOnly) para emitir novo par access + refresh. O refresh antigo é revogado (rotação).",
        responses = [
            ApiResponse(responseCode = "200", description = "Novo par de tokens emitido"),
            ApiResponse(responseCode = "401", description = "Refresh token ausente, expirado ou revogado")
        ]
    )
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
        refreshTokenService.criar(requireNotNull(usuario.id), newRawRefreshToken)

        val cookie = buildRefreshCookie(newRawRefreshToken, jwtService.getRefreshExpirationMs() / 1000, httpRequest.isSecure)
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                expiresIn = jwtService.getAccessExpirationMs() / 1000
            )
        )
    }

    @Operation(
        summary = "Logout",
        description = "Revoga o refresh token e limpa o cookie. O access token continua válido até expirar (15 min).",
        responses = [
            ApiResponse(responseCode = "204", description = "Logout realizado"),
            ApiResponse(responseCode = "401", description = "Não autenticado")
        ]
    )
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
            .secure(secure)
            .path("/api/v1/auth")
            .maxAge(maxAge)
            .sameSite("Lax")
            .build()
}
