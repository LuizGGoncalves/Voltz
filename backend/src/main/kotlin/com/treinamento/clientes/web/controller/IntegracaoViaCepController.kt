package com.treinamento.clientes.web.controller

import com.treinamento.clientes.integration.viacep.ViaCepHealthIndicator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/integracoes/viacep")
class IntegracaoViaCepController(
    private val viaCepHealthIndicator: ViaCepHealthIndicator
) {

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(
            mapOf(
                "disponivel" to viaCepHealthIndicator.isDisponivel(),
                "ultimaVerificacao" to viaCepHealthIndicator.getUltimaVerificacao().toString()
            )
        )
}
