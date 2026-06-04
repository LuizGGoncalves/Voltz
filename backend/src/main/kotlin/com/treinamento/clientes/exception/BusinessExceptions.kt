package com.treinamento.clientes.exception

class DocumentoDuplicadoException(documento: String) :
    RuntimeException("Documento já cadastrado: $documento")

class InstalacaoDuplicadaException(numeroInstalacao: String) :
    RuntimeException("Número de instalação já pertence a outro cliente: $numeroInstalacao")

class ClienteNaoEncontradoException(id: Long) :
    RuntimeException("Cliente não encontrado: $id")

class DocumentoInvalidoException(documento: String) :
    RuntimeException("Documento inválido (CPF ou CNPJ): $documento")
