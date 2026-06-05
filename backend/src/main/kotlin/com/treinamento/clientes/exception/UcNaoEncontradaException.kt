package com.treinamento.clientes.exception

class UcNaoEncontradaException(id: Long) :
    RuntimeException("Unidade consumidora não encontrada: $id")
