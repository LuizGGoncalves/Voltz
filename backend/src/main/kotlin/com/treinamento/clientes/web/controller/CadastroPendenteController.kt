package com.treinamento.clientes.web.controller

import com.treinamento.clientes.exception.ClienteNaoEncontradoException
import com.treinamento.clientes.repository.CadastroPendenteRepository
import com.treinamento.clientes.web.dto.CadastroPendenteResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cadastros-pendentes")
class CadastroPendenteController(
    private val repository: CadastroPendenteRepository
) {

    @GetMapping
    fun listar(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Page<CadastroPendenteResponse>> {
        val page = if (status != null) {
            repository.findAllByStatusOrderByCreatedAtDesc(status.uppercase(), pageable)
        } else {
            repository.findAllByOrderByCreatedAtDesc(pageable)
        }.map { it.toResponse() }
        return ResponseEntity.ok(page)
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<CadastroPendenteResponse> {
        val pendente = repository.findById(id)
            .orElseThrow { ClienteNaoEncontradoException(id) }
        return ResponseEntity.ok(pendente.toResponse())
    }

    private fun com.treinamento.clientes.domain.model.CadastroPendente.toResponse() =
        CadastroPendenteResponse(
            id = id!!,
            documento = documento,
            status = status,
            motivo = motivo,
            tentativas = tentativas,
            createdAt = createdAt,
            ultimaTentativa = ultimaTentativa
        )
}
