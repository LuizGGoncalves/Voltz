package com.treinamento.clientes.integration.viacep

import com.treinamento.clientes.domain.model.Endereco
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Service
class ViaCepService(
    @Value("\${viacep.timeout-ms:3000}")
    private val timeoutMs: Int
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient by lazy {
        RestClient.builder()
            .baseUrl("https://viacep.com.br/ws")
            .requestFactory(SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(timeoutMs)
                setReadTimeout(timeoutMs)
            })
            .build()
    }

    fun consultar(cep: String): Endereco {
        val cepLimpo = cep.replace("-", "").replace(".", "")

        val response = try {
            restClient.get()
                .uri("/{cep}/json", cepLimpo)
                .retrieve()
                .body(ViaCepResponse::class.java)
        } catch (ex: RestClientException) {
            log.warn("ViaCEP indisponível para CEP {}: {}", cepLimpo, ex.message)
            throw ViaCepIndisponivelException(cepLimpo, ex)
        }

        if (response == null || response.erro == true) {
            throw CepInvalidoException(cepLimpo)
        }

        return Endereco(
            cep = cepLimpo,
            logradouro = response.logradouro ?: "",
            numero = "",
            complemento = null,
            bairro = response.bairro ?: "",
            cidade = response.localidade ?: "",
            uf = response.uf ?: ""
        )
    }
}

class ViaCepIndisponivelException(cep: String, cause: Throwable? = null) :
    RuntimeException("ViaCEP indisponível ao consultar CEP: $cep", cause)

class CepInvalidoException(cep: String) :
    RuntimeException("CEP não encontrado: $cep")
