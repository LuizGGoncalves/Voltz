package com.treinamento.clientes

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@EnableMethodSecurity
class ClientesApplication

fun main(args: Array<String>) {
	runApplication<ClientesApplication>(*args)
}
