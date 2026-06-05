package com.treinamento.clientes.web.controller

import com.treinamento.clientes.service.AnaliseMgService
import com.treinamento.clientes.web.dto.AnaliseClienteMgResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/analises-mg")
class AnaliseMgController(
    private val service: AnaliseMgService
) {

    @GetMapping
    fun listar(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<AnaliseClienteMgResponse>> =
        ResponseEntity.ok(service.listar(pageable))
}
