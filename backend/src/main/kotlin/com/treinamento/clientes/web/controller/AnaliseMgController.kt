package com.treinamento.clientes.web.controller

import com.treinamento.clientes.service.AnaliseMgService
import com.treinamento.clientes.web.dto.AnaliseClienteMgResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Análises MG", description = "Eventos de análise para UCs em Minas Gerais")
@RestController
@RequestMapping("/api/v1/analises-mg")
class AnaliseMgController(
    private val service: AnaliseMgService
) {

    @Operation(
        summary = "Listar análises MG",
        description = "Lista paginada de eventos gerados automaticamente quando uma UC é cadastrada em MG. Cada registro vincula um cliente a uma UC.",
        responses = [
            ApiResponse(responseCode = "200", description = "Página de análises MG")
        ]
    )
    @GetMapping
    fun listar(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<AnaliseClienteMgResponse>> =
        ResponseEntity.ok(service.listar(pageable))
}
