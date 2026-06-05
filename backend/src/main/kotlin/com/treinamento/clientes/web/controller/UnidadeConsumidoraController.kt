package com.treinamento.clientes.web.controller

import com.treinamento.clientes.service.UnidadeConsumidoraService
import com.treinamento.clientes.web.dto.UnidadeConsumidoraRequest
import com.treinamento.clientes.web.dto.UnidadeConsumidoraResponse
import com.treinamento.clientes.web.mapper.toResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/clientes/{clienteId}/unidades")
class UnidadeConsumidoraController(
    private val ucService: UnidadeConsumidoraService
) {

    @PreAuthorize("hasRole('USER')")
    @GetMapping
    fun listar(@PathVariable clienteId: Long): ResponseEntity<List<UnidadeConsumidoraResponse>> {
        val ucs = ucService.listarPorCliente(clienteId).map { it.toResponse() }
        return ResponseEntity.ok(ucs)
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    fun adicionar(
        @PathVariable clienteId: Long,
        @Valid @RequestBody request: UnidadeConsumidoraRequest
    ): ResponseEntity<UnidadeConsumidoraResponse> {
        val uc = ucService.adicionar(clienteId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(uc.toResponse())
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{ucId}")
    fun atualizar(
        @PathVariable clienteId: Long,
        @PathVariable ucId: Long,
        @Valid @RequestBody request: UnidadeConsumidoraRequest
    ): ResponseEntity<UnidadeConsumidoraResponse> {
        val uc = ucService.atualizar(clienteId, ucId, request)
        return ResponseEntity.ok(uc.toResponse())
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{ucId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@PathVariable clienteId: Long, @PathVariable ucId: Long) {
        ucService.remover(clienteId, ucId)
    }
}
