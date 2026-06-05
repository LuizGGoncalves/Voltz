package com.treinamento.clientes.web.controller

import com.treinamento.clientes.service.CadastroPendenteService
import com.treinamento.clientes.web.dto.CadastroPendenteResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Cadastros Pendentes", description = "Fila de cadastros aguardando validação do ViaCEP")
@RestController
@RequestMapping("/api/v1/cadastros-pendentes")
class CadastroPendenteController(
    private val service: CadastroPendenteService
) {

    @Operation(
        summary = "Listar cadastros pendentes",
        description = "Lista paginada da fila de retry. Filtre por status: PENDENTE, PROCESSADO, REJEITADO ou FALHA. Quando PROCESSADO, o campo `clienteId` indica o cliente criado.",
        responses = [
            ApiResponse(responseCode = "200", description = "Página de cadastros pendentes")
        ]
    )
    @GetMapping
    fun listar(
        @PageableDefault(size = 20) pageable: Pageable,
        @Parameter(description = "Filtrar por status: PENDENTE, PROCESSADO, REJEITADO, FALHA")
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Page<CadastroPendenteResponse>> =
        ResponseEntity.ok(service.listar(pageable, status))

    @Operation(
        summary = "Buscar cadastro pendente por ID",
        responses = [
            ApiResponse(responseCode = "200", description = "Cadastro encontrado"),
            ApiResponse(responseCode = "404", description = "Cadastro não encontrado")
        ]
    )
    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<CadastroPendenteResponse> =
        ResponseEntity.ok(service.buscarPorId(id))
}
