package com.treinamento.clientes.service

import com.treinamento.clientes.domain.event.AnaliseClienteMgEvent
import com.treinamento.clientes.domain.model.UnidadeConsumidora
import com.treinamento.clientes.domain.rules.UfRules
import com.treinamento.clientes.exception.ClienteNaoEncontradoException
import com.treinamento.clientes.exception.InstalacaoDuplicadaException
import com.treinamento.clientes.exception.UcNaoEncontradaException
import com.treinamento.clientes.exception.UfBloqueadaException
import com.treinamento.clientes.integration.viacep.ViaCepService
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.repository.UnidadeConsumidoraRepository
import com.treinamento.clientes.web.dto.UnidadeConsumidoraRequest
import com.treinamento.clientes.web.mapper.toModel
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UnidadeConsumidoraService(
    private val ucRepository: UnidadeConsumidoraRepository,
    private val clienteRepository: ClienteRepository,
    private val viaCepService: ViaCepService,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)


    @Transactional
    fun adicionar(clienteId: Long, request: UnidadeConsumidoraRequest): UnidadeConsumidora {
        val cliente = clienteRepository.findById(clienteId)
            .orElseThrow { ClienteNaoEncontradoException(clienteId) }

        validarInstalacaoUnica(request.numeroInstalacao)

        val uc = request.toModel()
        val enderecoEnriquecido = viaCepService.consultar(request.endereco.cep)
        uc.endereco.enriquecerCom(enderecoEnriquecido)

        aplicarRegraUf(uc)

        uc.cliente = cliente
        val salva = ucRepository.save(uc)

        verificarEventoMg(salva, clienteId)
        return salva
    }

    @Transactional
    fun atualizar(clienteId: Long, ucId: Long, request: UnidadeConsumidoraRequest): UnidadeConsumidora {
        val uc = buscarOuFalhar(clienteId, ucId)

        if (uc.numeroInstalacao != request.numeroInstalacao) {
            validarInstalacaoUnica(request.numeroInstalacao)
        }

        uc.nome = request.nome
        uc.numeroInstalacao = request.numeroInstalacao
        uc.endereco = request.endereco.toModel()

        val enderecoEnriquecido = viaCepService.consultar(request.endereco.cep)
        uc.endereco.enriquecerCom(enderecoEnriquecido)

        aplicarRegraUf(uc)

        val salva = ucRepository.save(uc)
        verificarEventoMg(salva, clienteId)
        return salva
    }

    @Transactional
    fun remover(clienteId: Long, ucId: Long) {
        val uc = buscarOuFalhar(clienteId, ucId)
        ucRepository.delete(uc)
    }

    @Transactional(readOnly = true)
    fun listarPorCliente(clienteId: Long): List<UnidadeConsumidora> {
        if (!clienteRepository.existsById(clienteId)) throw ClienteNaoEncontradoException(clienteId)
        return ucRepository.findByClienteIdAndAtivoTrue(clienteId)
    }

    private fun buscarOuFalhar(clienteId: Long, ucId: Long): UnidadeConsumidora {
        val uc = ucRepository.findById(ucId).orElseThrow { UcNaoEncontradaException(ucId) }
        if (uc.cliente?.id != clienteId) throw UcNaoEncontradaException(ucId)
        return uc
    }

    private fun validarInstalacaoUnica(numeroInstalacao: String) {
        if (ucRepository.existsByNumeroInstalacaoAndAtivoTrue(numeroInstalacao)) {
            throw InstalacaoDuplicadaException(numeroInstalacao)
        }
    }

    private fun aplicarRegraUf(uc: UnidadeConsumidora) {
        val uf = uc.endereco.uf.uppercase()
        if (uf in UfRules.BLOQUEADAS) {
            throw UfBloqueadaException(uf, uc.nome)
        }
    }

    private fun verificarEventoMg(uc: UnidadeConsumidora, clienteId: Long) {
        if (uc.endereco.uf.uppercase() == UfRules.EVENTO_MG) {
            log.info("UC '{}' em MG — publicando evento analise_cliente_mg", uc.nome)
            eventPublisher.publishEvent(
                AnaliseClienteMgEvent(clienteId = clienteId, unidadeConsumidoraId = requireNotNull(uc.id))
            )
        }
    }

}
