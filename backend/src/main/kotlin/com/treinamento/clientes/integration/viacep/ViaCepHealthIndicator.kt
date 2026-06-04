package com.treinamento.clientes.integration.viacep

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Component("viaCep")
class ViaCepHealthIndicator(
    private val viaCepService: ViaCepService
) : HealthIndicator {

    private val log = LoggerFactory.getLogger(javaClass)
    private val disponivel = AtomicBoolean(true)
    private val ultimaVerificacao = AtomicReference(Instant.now())

    @Scheduled(fixedDelayString = "\${viacep.health.interval-ms:30000}")
    fun verificar() {
        try {
            viaCepService.consultar("01001000") // CEP conhecido (Praça da Sé)
            disponivel.set(true)
        } catch (ex: Exception) {
            log.warn("ViaCEP health check falhou: {}", ex.message)
            disponivel.set(false)
        }
        ultimaVerificacao.set(Instant.now())
    }

    override fun health(): Health {
        val builder = if (disponivel.get()) Health.up() else Health.down()
        return builder
            .withDetail("disponivel", disponivel.get())
            .withDetail("ultimaVerificacao", ultimaVerificacao.get().toString())
            .build()
    }

    fun isDisponivel(): Boolean = disponivel.get()
    fun getUltimaVerificacao(): Instant = ultimaVerificacao.get()
}
