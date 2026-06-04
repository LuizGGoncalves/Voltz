package com.treinamento.clientes.exception

import com.treinamento.clientes.integration.viacep.CepInvalidoException
import com.treinamento.clientes.integration.viacep.ViaCepIndisponivelException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ClienteNaoEncontradoException::class)
    fun handleNotFound(ex: ClienteNaoEncontradoException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message!!)

    @ExceptionHandler(DocumentoDuplicadoException::class)
    fun handleConflict(ex: DocumentoDuplicadoException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message!!)

    @ExceptionHandler(InstalacaoDuplicadaException::class)
    fun handleInstalacaoConflict(ex: InstalacaoDuplicadaException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message!!)

    @ExceptionHandler(DocumentoInvalidoException::class)
    fun handleDocumentoInvalido(ex: DocumentoInvalidoException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message!!)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Falha de validação")
        val errors = ex.bindingResult.fieldErrors.map { mapOf("field" to it.field, "message" to it.defaultMessage) }
        problem.setProperty("errors", errors)
        return problem
    }

    @ExceptionHandler(UfBloqueadaException::class)
    fun handleUfBloqueada(ex: UfBloqueadaException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.message!!)

    @ExceptionHandler(CepNaoEncontradoException::class)
    fun handleCepNaoEncontrado(ex: CepNaoEncontradoException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.message!!)

    @ExceptionHandler(CepInvalidoException::class)
    fun handleCepInvalido(ex: CepInvalidoException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.message!!)

    @ExceptionHandler(ViaCepIndisponivelException::class)
    fun handleViaCepIndisponivel(ex: ViaCepIndisponivelException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.message!!)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Violação de integridade: registro duplicado ou referência inválida")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Requisição inválida")
}
