package com.treinamento.clientes.web.controller

import com.treinamento.clientes.integration.viacep.ViaCepIndisponivelException
import com.treinamento.clientes.service.CadastroPendenteService
import com.treinamento.clientes.service.ClienteService
import com.treinamento.clientes.web.dto.*
import com.treinamento.clientes.web.mapper.toResponse
import com.treinamento.clientes.web.mapper.toResumoResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "Clientes", description = "CRUD de clientes com exclusão lógica, paginação e ordenação")
@RestController
@RequestMapping("/api/v1/clientes")
class ClienteController(
    private val clienteService: ClienteService,
    private val cadastroPendenteService: CadastroPendenteService
) {

    @Operation(
        summary = "Criar cliente",
        description = "Cria um cliente com pelo menos 1 UC. Se o ViaCEP estiver fora do ar, enfileira para processamento automático (retorna 202). UCs em SP/RS/PR são bloqueadas (422). UCs em MG geram evento de análise.",
        responses = [
            ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
            ApiResponse(responseCode = "202", description = "ViaCEP indisponível — cadastro enfileirado para retry"),
            ApiResponse(responseCode = "400", description = "Dados inválidos (documento, campos obrigatórios)"),
            ApiResponse(responseCode = "409", description = "Documento ou nº instalação já cadastrado"),
            ApiResponse(responseCode = "422", description = "UF bloqueada (SP/RS/PR) ou CEP não encontrado")
        ]
    )
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

    @Operation(
        summary = "Atualizar cliente",
        description = "Atualiza nome, documento e endereço do cliente. Não altera UCs (use os endpoints de UC). O endereço é re-enriquecido pelo ViaCEP.",
        responses = [
            ApiResponse(responseCode = "200", description = "Cliente atualizado"),
            ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            ApiResponse(responseCode = "409", description = "Documento duplicado ou conflito de versão")
        ]
    )
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: Long,
        @Valid @RequestBody request: ClienteUpdateRequest
    ): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.atualizar(id, request)
        return ResponseEntity.ok(cliente.toResponse())
    }

    @Operation(
        summary = "Buscar cliente por ID",
        description = "Retorna os dados completos do cliente incluindo todas as suas UCs ativas.",
        responses = [
            ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            ApiResponse(responseCode = "404", description = "Cliente não encontrado")
        ]
    )
    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.buscarPorId(id)
        return ResponseEntity.ok(cliente.toResponse())
    }

    @Operation(
        summary = "Listar clientes",
        description = "Lista paginada com filtro por status e ordenação. Use `sort=createdAt,desc` para ver os mais recentes primeiro.",
        responses = [
            ApiResponse(responseCode = "200", description = "Página de clientes")
        ]
    )
    @GetMapping
    fun listar(
        @PageableDefault(size = 20, sort = ["nome"]) pageable: Pageable,
        @Parameter(description = "Filtro: ativos, inativos ou todos (default: só ativos)")
        @RequestParam(required = false) filtroStatus: String?,
        @Parameter(description = "Retrocompatibilidade — usar filtroStatus preferencialmente")
        @RequestParam(defaultValue = "false") incluirInativos: Boolean
    ): ResponseEntity<Page<ClienteResumoResponse>> {
        val page = clienteService.listar(pageable, filtroStatus, incluirInativos).map { it.toResumoResponse() }
        return ResponseEntity.ok(page)
    }

    @Operation(
        summary = "Corrigir documento (ADMIN)",
        description = "Altera o CPF/CNPJ de um cliente. Restrito a ADMIN. Grava auditoria (quem, quando, de/para, motivo).",
        responses = [
            ApiResponse(responseCode = "200", description = "Documento corrigido"),
            ApiResponse(responseCode = "400", description = "Documento inválido"),
            ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)"),
            ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
            ApiResponse(responseCode = "409", description = "Documento duplicado")
        ]
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/documento")
    fun corrigirDocumento(
        @PathVariable id: Long,
        @Valid @RequestBody request: CorrecaoDocumentoRequest
    ): ResponseEntity<ClienteResponse> {
        val cliente = clienteService.corrigirDocumento(id, request.documento, request.motivo)
        return ResponseEntity.ok(cliente.toResponse())
    }

    @Operation(
        summary = "Inativar cliente (ADMIN)",
        description = "Exclusão lógica — marca o cliente como inativo. Os dados são preservados para auditoria. Restrito a ADMIN.",
        responses = [
            ApiResponse(responseCode = "204", description = "Cliente inativado"),
            ApiResponse(responseCode = "403", description = "Sem permissão (requer ADMIN)"),
            ApiResponse(responseCode = "404", description = "Cliente não encontrado")
        ]
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun inativar(@PathVariable id: Long) {
        clienteService.inativar(id)
    }
}
