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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant

@Component
class RetryCadastroJob(
    private val cadastroPendenteRepository: CadastroPendenteRepository,
    private val clienteService: ClienteService,
    private val viaCepService: ViaCepService,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager
) {
    private val txTemplate = TransactionTemplate(transactionManager)

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_TENTATIVAS = 5
        const val BATCH_SIZE = 50
        const val MAX_FALHAS_CONSECUTIVAS = 3
        val TTL: Duration = Duration.ofHours(24)
        val BACKOFF_INTERVALS = longArrayOf(1, 2, 4, 8, 16) // minutos
    }

    private var falhasConsecutivas = 0

    @Scheduled(fixedDelayString = "\${retry.interval-ms:60000}")
    fun processarPendentes() {
        val pendentes = cadastroPendenteRepository.findPendentesParaRetry(BATCH_SIZE)
        if (pendentes.isEmpty()) return

        log.info("Retry job: {} pendentes para processar", pendentes.size)
        var falhasNoBatch = 0

        pendentes.forEach { pendente ->
            try {
                txTemplate.executeWithoutResult { processarUm(pendente) }
                falhasConsecutivas = 0
            } catch (ex: Exception) {
                falhasNoBatch++
                log.error("Erro inesperado ao processar pendente {}: {}", pendente.id, ex.message)
            }
        }

        if (falhasNoBatch == pendentes.size) {
            falhasConsecutivas++
            log.warn("Retry job: batch inteiro falhou ({} consecutivas). Possível problema sistêmico.", falhasConsecutivas)
            if (falhasConsecutivas >= MAX_FALHAS_CONSECUTIVAS) {
                log.error("ALERTA: {} batches consecutivos falharam integralmente. Verificar saúde do sistema.", falhasConsecutivas)
            }
        }
    }

    private fun processarUm(pendente: CadastroPendente) {
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
            cliente.endereco.enriquecerCom(viaCepService.consultar(request.endereco.cep))

            cliente.unidadesConsumidoras.forEachIndexed { i, uc ->
                uc.endereco.enriquecerCom(viaCepService.consultar(request.unidadesConsumidoras[i].endereco.cep))
            }

            // Fonte única das regras de UF
            val salvo = clienteService.finalizarCadastro(cliente)

            pendente.status = "PROCESSADO"
            pendente.motivo = null
            pendente.clienteId = salvo.id
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
        val proximoRetry = requireNotNull(pendente.ultimaTentativa).plusSeconds(intervaloMinutos * 60)
        return Instant.now().isAfter(proximoRetry)
    }
}
