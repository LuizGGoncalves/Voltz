package com.treinamento.clientes.web.controller

import com.treinamento.clientes.service.UnidadeConsumidoraService
import com.treinamento.clientes.web.dto.UnidadeConsumidoraRequest
import com.treinamento.clientes.web.dto.UnidadeConsumidoraResponse
import com.treinamento.clientes.web.mapper.toResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Unidades Consumidoras", description = "CRUD de UCs vinculadas a um cliente")
@RestController
@RequestMapping("/api/v1/clientes/{clienteId}/unidades")
class UnidadeConsumidoraController(
    private val ucService: UnidadeConsumidoraService
) {

    @Operation(
        summary = "Listar UCs do cliente",
        description = "Retorna todas as UCs ativas vinculadas ao cliente.",
        responses = [
            ApiResponse(responseCode = "200", description = "Lista de UCs"),
            ApiResponse(responseCode = "404", description = "Cliente não encontrado")
        ]
    )
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    fun listar(@PathVariable clienteId: Long): ResponseEntity<List<UnidadeConsumidoraResponse>> {
        val ucs = ucService.listarPorCliente(clienteId).map { it.toResponse() }
        return ResponseEntity.ok(ucs)
    }

    @Operation(
        summary = "Adicionar UC",
        description = "Cria uma nova UC vinculada ao cliente. O endereço é enriquecido pelo ViaCEP. UCs em SP/RS/PR são bloqueadas. UCs em MG geram evento de análise.",
        responses = [
            ApiResponse(responseCode = "201", description = "UC criada"),
            ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            ApiResponse(responseCode = "409", description = "Nº instalação duplicado"),
            ApiResponse(responseCode = "422", description = "UF bloqueada (SP/RS/PR)"),
            ApiResponse(responseCode = "503", description = "ViaCEP indisponível")
        ]
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    fun adicionar(
        @PathVariable clienteId: Long,
        @Valid @RequestBody request: UnidadeConsumidoraRequest
    ): ResponseEntity<UnidadeConsumidoraResponse> {
        val uc = ucService.adicionar(clienteId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(uc.toResponse())
    }

    @Operation(
        summary = "Atualizar UC",
        description = "Atualiza nome, nº instalação e endereço da UC. O endereço é re-enriquecido pelo ViaCEP.",
        responses = [
            ApiResponse(responseCode = "200", description = "UC atualizada"),
            ApiResponse(responseCode = "404", description = "Cliente ou UC não encontrado"),
            ApiResponse(responseCode = "409", description = "Nº instalação duplicado"),
            ApiResponse(responseCode = "422", description = "UF bloqueada")
        ]
    )
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

    @Operation(
        summary = "Remover UC (ADMIN)",
        description = "Exclusão lógica da UC. Restrito a ADMIN.",
        responses = [
            ApiResponse(responseCode = "204", description = "UC removida"),
            ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)"),
            ApiResponse(responseCode = "404", description = "Cliente ou UC não encontrado")
        ]
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{ucId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remover(@PathVariable clienteId: Long, @PathVariable ucId: Long) {
        ucService.remover(clienteId, ucId)
    }
}
