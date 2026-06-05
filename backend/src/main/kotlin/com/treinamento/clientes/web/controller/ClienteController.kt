package com.treinamento.clientes.web.controller

import com.treinamento.clientes.integration.viacep.ViaCepIndisponivelException
import com.treinamento.clientes.service.CadastroPendenteService
import com.treinamento.clientes.service.ClienteService
import com.treinamento.clientes.web.dto.*
import com.treinamento.clientes.web.mapper.toResponse
import com.treinamento.clientes.web.mapper.toResumoResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/clientes")
class ClienteController(
    private val clienteService: ClienteService,
    private val cadastroPendenteService: CadastroPendenteService
) {

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    fun criar(@Valid @RequestBody request: ClienteRequest): ResponseEntity<*> {
        return try {
            val cliente = clienteService.criar(request)
            ResponseEntity.status(HttpStatus.CREATED).body(cliente.toResponse())
        } catch (ex: ViaCepIndisponivelException) {
            val pendente = cadastroPendenteService.enfileirar(request)
            ResponseEntity.status(HttpStatus.ACCEPTED).body(
                CadastroPendenteCreatedResponse(cadastroPendenteId = requireNotNull(pendente.id))
            )
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: ClienteUpdateRequest
    ): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.atualizar(id, request)
        return ResponseEntity.ok(cliente.toResponse())
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.buscarPorId(id)
        return ResponseEntity.ok(cliente.toResponse())
    }

    @GetMapping
    fun listar(
        @PageableDefault(size = 20, sort = ["nome"]) pageable: Pageable,
        @RequestParam(required = false) filtroStatus: String?,
        @RequestParam(defaultValue = "false") incluirInativos: Boolean
    ): ResponseEntity<Page<ClienteResumoResponse>> {
        val page = clienteService.listar(pageable, filtroStatus, incluirInativos).map { it.toResumoResponse() }
        return ResponseEntity.ok(page)
    }

    @GetMapping("/ultimos")
    fun ultimos20(): ResponseEntity<Page<ClienteResumoResponse>> {
        val page = clienteService.ultimos20().map { it.toResumoResponse() }
        return ResponseEntity.ok(page)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/documento")
    fun corrigirDocumento(
        @PathVariable id: Long,
        @Valid @RequestBody request: CorrecaoDocumentoRequest
    ): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.corrigirDocumento(id, request.documento, request.motivo)
        return ResponseEntity.ok(cliente.toResponse())
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun inativar(@PathVariable id: Long) {
        clienteService.inativar(id)
    }
}
