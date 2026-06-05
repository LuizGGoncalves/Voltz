package com.treinamento.clientes.web.controller

import com.treinamento.clientes.service.CadastroPendenteService
import com.treinamento.clientes.web.dto.CadastroPendenteResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cadastros-pendentes")
class CadastroPendenteController(
    private val service: CadastroPendenteService
) {

    @GetMapping
    fun listar(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Page<CadastroPendenteResponse>> =
        ResponseEntity.ok(service.listar(pageable, status))

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<CadastroPendenteResponse> =
        ResponseEntity.ok(service.buscarPorId(id))
}
