package com.treinamento.clientes.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.treinamento.clientes.security.JwtAuthenticationConverter
import com.treinamento.clientes.security.JwtProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.crypto.spec.SecretKeySpec

@Configuration
class SecurityConfig(
    private val jwtProperties: JwtProperties,
    private val jwtAuthenticationConverter: JwtAuthenticationConverter,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/v1/integracoes/viacep/status").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/clientes/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/clientes/*/documento").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    val problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Token ausente ou inválido")
                    objectMapper.writeValue(response.outputStream, problem)
                }
                ex.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    val problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado")
                    objectMapper.writeValue(response.outputStream, problem)
                }
            }
            .build()

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val keyBytes = jwtProperties.secret.toByteArray()
        val padded = if (keyBytes.size < 32) keyBytes.copyOf(32) else keyBytes
        val secretKey = SecretKeySpec(padded, "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:4200")
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
