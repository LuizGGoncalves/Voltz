package com.treinamento.clientes.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimitService {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    /**
     * Verifica se a chave (IP ou username) pode fazer mais uma tentativa.
     * Limite: 5 tentativas a cada 15 minutos por chave.
     */
    fun tryConsume(key: String): Boolean =
        buckets.computeIfAbsent(key) { createBucket() }.tryConsume(1)

    private fun createBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(5)
                    .refillGreedy(5, Duration.ofMinutes(15))
                    .build()
            )
            .build()
}
