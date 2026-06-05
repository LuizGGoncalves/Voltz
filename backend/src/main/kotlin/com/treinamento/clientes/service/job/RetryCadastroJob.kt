package com.treinamento.clientes.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.treinamento.clientes.domain.model.CadastroPendente
import com.treinamento.clientes.exception.UfBloqueadaException
import com.treinamento.clientes.integration.viacep.ViaCepIndisponivelException
import com.treinamento.clientes.integration.viacep.ViaCepService
import com.treinamento.clientes.repository.CadastroPendenteRepository
import com.treinamento.clientes.service.ClienteService
import com.treinamento.clientes.web.dto.ClienteRequest
import com.treinamento.clientes.web.mapper.toModel
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Component
class RetryCadastroJob(
    private val cadastroPendenteRepository: CadastroPendenteRepository,
    private val clienteService: ClienteService,
    private val viaCepService: ViaCepService,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_TENTATIVAS = 5
        const val BATCH_SIZE = 50
        val TTL: Duration = Duration.ofHours(24)
        val BACKOFF_INTERVALS = longArrayOf(1, 2, 4, 8, 16) // minutos
    }

    @Scheduled(fixedDelayString = "\${retry.interval-ms:60000}")
    fun processarPendentes() {
        val pendentes = cadastroPendenteRepository.findPendentesParaRetry(BATCH_SIZE)
        if (pendentes.isEmpty()) return

        log.info("Retry job: {} pendentes para processar", pendentes.size)
        pendentes.forEach { pendente ->
            try {
                processarUm(pendente)
            } catch (ex: Exception) {
                log.error("Erro inesperado ao processar pendente {}: {}", pendente.id, ex.message)
            }
        }
    }

    @Transactional
    fun processarUm(pendente: CadastroPendente) {
        // TTL expirado
        if (pendente.createdAt.plus(TTL).isBefore(Instant.now())) {
            pendente.status = "FALHA"
            pendente.motivo = "TTL expirado (24h)"
            cadastroPendenteRepository.save(pendente)
            log.warn("Pendente {} expirado por TTL", pendente.id)
            return
        }

        // Backoff: só processa se o intervalo já passou
        if (!elegívelParaRetry(pendente)) return

        pendente.tentativas++
        pendente.ultimaTentativa = Instant.now()

        try {
            val request = objectMapper.readValue(pendente.payload, ClienteRequest::class.java)
            val cliente = request.toModel()

            // Enriquecer via ViaCEP
            val endCliente = viaCepService.consultar(request.endereco.cep)
            cliente.endereco.logradouro = endCliente.logradouro
            cliente.endereco.bairro = endCliente.bairro
            cliente.endereco.cidade = endCliente.cidade
            cliente.endereco.uf = endCliente.uf

            cliente.unidadesConsumidoras.forEachIndexed { i, uc ->
                val endUc = viaCepService.consultar(request.unidadesConsumidoras[i].endereco.cep)
                uc.endereco.logradouro = endUc.logradouro
                uc.endereco.bairro = endUc.bairro
                uc.endereco.cidade = endUc.cidade
                uc.endereco.uf = endUc.uf
            }

            // Fonte única das regras de UF
            clienteService.finalizarCadastro(cliente)

            pendente.status = "PROCESSADO"
            pendente.motivo = null
            log.info("Pendente {} processado com sucesso → Cliente criado", pendente.id)

        } catch (ex: ViaCepIndisponivelException) {
            if (pendente.tentativas >= MAX_TENTATIVAS) {
                pendente.status = "FALHA"
                pendente.motivo = "Máximo de tentativas atingido (ViaCEP indisponível)"
                log.warn("Pendente {} → FALHA após {} tentativas", pendente.id, pendente.tentativas)
            } else {
                log.info("Pendente {} — ViaCEP ainda indisponível, tentativa {}/{}", pendente.id, pendente.tentativas, MAX_TENTATIVAS)
            }
        } catch (ex: UfBloqueadaException) {
            pendente.status = "REJEITADO"
            pendente.motivo = ex.message
            log.info("Pendente {} → REJEITADO: {}", pendente.id, ex.message)
        } catch (ex: Exception) {
            pendente.status = "FALHA"
            pendente.motivo = ex.message?.take(500)
            log.error("Pendente {} → FALHA: {}", pendente.id, ex.message)
        }

        cadastroPendenteRepository.save(pendente)
    }

    private fun elegívelParaRetry(pendente: CadastroPendente): Boolean {
        if (pendente.ultimaTentativa == null) return true
        val intervaloMinutos = BACKOFF_INTERVALS.getOrElse(pendente.tentativas) { BACKOFF_INTERVALS.last() }
        val proximoRetry = pendente.ultimaTentativa!!.plusSeconds(intervaloMinutos * 60)
        return Instant.now().isAfter(proximoRetry)
    }
}
