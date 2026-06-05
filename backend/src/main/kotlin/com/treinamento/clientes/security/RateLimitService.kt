package com.treinamento.clientes.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimitService(
    @Value("\${rate-limit.login.capacity:5}")
    private val capacity: Long,
    @Value("\${rate-limit.login.refill-minutes:15}")
    private val refillMinutes: Long
) {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(key: String): Boolean =
        buckets.computeIfAbsent(key) { createBucket() }.tryConsume(1)

    private fun createBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(capacity, Duration.ofMinutes(refillMinutes))
                    .build()
            )
            .build()
}
