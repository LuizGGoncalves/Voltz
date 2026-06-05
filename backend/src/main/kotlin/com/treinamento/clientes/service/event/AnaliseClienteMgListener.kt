package com.treinamento.clientes.service.event

import com.treinamento.clientes.domain.event.AnaliseClienteMgEvent
import com.treinamento.clientes.domain.model.AnaliseClienteMg
import com.treinamento.clientes.repository.AnaliseClienteMgRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AnaliseClienteMgListener(
    private val repository: AnaliseClienteMgRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAnaliseClienteMg(event: AnaliseClienteMgEvent) {
        try {
            log.info("Registrando análise MG — cliente={}, uc={}", event.clienteId, event.unidadeConsumidoraId)
            repository.save(
                AnaliseClienteMg(
                    clienteId = event.clienteId,
                    unidadeConsumidoraId = event.unidadeConsumidoraId
                )
            )
        } catch (ex: Exception) {
            log.error("Falha ao registrar análise MG — cliente={}, uc={}: {}",
                event.clienteId, event.unidadeConsumidoraId, ex.message, ex)
        }
    }
}
