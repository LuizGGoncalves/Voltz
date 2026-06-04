package com.treinamento.clientes.web.mapper

import com.treinamento.clientes.domain.model.Cliente
import com.treinamento.clientes.domain.model.Endereco
import com.treinamento.clientes.domain.model.UnidadeConsumidora
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.web.dto.*

fun EnderecoRequest.toModel(): Endereco = Endereco(
    cep = cep.replace("-", ""),
    numero = numero,
    complemento = complemento
)

fun Endereco.toResponse(): EnderecoResponse = EnderecoResponse(
    cep = cep,
    logradouro = logradouro,
    numero = numero,
    complemento = complemento,
    bairro = bairro,
    cidade = cidade,
    uf = uf
)

fun UnidadeConsumidoraRequest.toModel(): UnidadeConsumidora = UnidadeConsumidora(
    nome = nome,
    numeroInstalacao = numeroInstalacao,
    endereco = endereco.toModel()
)

fun UnidadeConsumidora.toResponse(): UnidadeConsumidoraResponse = UnidadeConsumidoraResponse(
    id = id!!,
    nome = nome,
    numeroInstalacao = numeroInstalacao,
    endereco = endereco.toResponse(),
    ativo = ativo
)

fun ClienteRequest.toModel(): Cliente {
    val cliente = Cliente(
        nome = nome,
        documento = Documento.of(documento),
        endereco = endereco.toModel()
    )
    unidadesConsumidoras.forEach { ucReq ->
        cliente.adicionarUC(ucReq.toModel())
    }
    return cliente
}

fun Cliente.toResponse(): ClienteResponse = ClienteResponse(
    id = id!!,
    nome = nome,
    documento = documento!!.valor,
    tipoDocumento = documento!!.tipo.name,
    ativo = ativo,
    createdAt = createdAt,
    updatedAt = updatedAt,
    endereco = endereco.toResponse(),
    unidadesConsumidoras = unidadesConsumidoras.map { it.toResponse() }
)

fun Cliente.toResumoResponse(): ClienteResumoResponse = ClienteResumoResponse(
    id = id!!,
    nome = nome,
    documento = documento!!.valor,
    tipoDocumento = documento!!.tipo.name,
    ativo = ativo,
    createdAt = createdAt,
    updatedAt = updatedAt
)
