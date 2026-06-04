package com.treinamento.clientes.exception

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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Requisição inválida")
}
