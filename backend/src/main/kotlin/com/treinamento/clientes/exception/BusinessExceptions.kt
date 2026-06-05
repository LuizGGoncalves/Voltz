package com.treinamento.clientes.exception

class DocumentoDuplicadoException(documento: String) :
    RuntimeException("Documento já cadastrado: $documento")

class InstalacaoDuplicadaException(numeroInstalacao: String) :
    RuntimeException("Número de instalação já pertence a outro cliente: $numeroInstalacao")

class ClienteNaoEncontradoException(id: Long) :
    RuntimeException("Cliente não encontrado: $id")

class DocumentoInvalidoException(documento: String) :
    RuntimeException("Documento inválido (CPF ou CNPJ): $documento")

class UfBloqueadaException(uf: String, ucNome: String) :
    RuntimeException("Unidade consumidora '$ucNome' em $uf não é permitida.")

class CepNaoEncontradoException(cep: String) :
    RuntimeException("CEP não encontrado: $cep")

class CadastroPendenteNaoEncontradoException(id: Long) :
    RuntimeException("Cadastro pendente não encontrado: $id")

class LimiteTentativasExcedidoException :
    RuntimeException("Muitas tentativas. Tente novamente em alguns minutos.")
