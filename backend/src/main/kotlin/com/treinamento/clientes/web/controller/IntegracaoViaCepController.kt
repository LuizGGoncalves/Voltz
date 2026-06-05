package com.treinamento.clientes.web.controller

import com.treinamento.clientes.integration.viacep.ViaCepHealthIndicator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Integrações", description = "Status de serviços externos")
@RestController
@RequestMapping("/api/v1/integracoes/viacep")
class IntegracaoViaCepController(
    private val viaCepHealthIndicator: ViaCepHealthIndicator
) {

    @Operation(
        summary = "Status do ViaCEP",
        description = "Verifica se a API do ViaCEP está disponível. Atualizado a cada 30 segundos. Quando indisponível, cadastros são enfileirados automaticamente.",
        responses = [
            ApiResponse(responseCode = "200", description = "Status retornado com sucesso")
        ]
    )
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(
            mapOf(
                "disponivel" to viaCepHealthIndicator.isDisponivel(),
                "ultimaVerificacao" to viaCepHealthIndicator.getUltimaVerificacao().toString()
            )
        )
}
