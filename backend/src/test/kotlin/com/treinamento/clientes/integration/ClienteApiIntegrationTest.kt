package com.treinamento.clientes.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.treinamento.clientes.web.dto.ClienteRequest
import com.treinamento.clientes.web.dto.EnderecoRequest
import com.treinamento.clientes.web.dto.LoginRequest
import com.treinamento.clientes.web.dto.UnidadeConsumidoraRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*

class ClienteApiIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private var token: String = ""

    @BeforeEach
    fun login() {
        val loginReq = LoginRequest("admin", "admin123")
        val response = restTemplate.postForEntity("/api/v1/auth/login", loginReq, Map::class.java)
        token = response.body?.get("accessToken") as? String ?: ""
    }

    private fun authHeaders(): HttpHeaders = HttpHeaders().apply {
        setBearerAuth(token)
        contentType = MediaType.APPLICATION_JSON
    }

    private fun criarClienteRequest(doc: String = "39053344705", ucCep: String = "20040020") =
        ClienteRequest(
            nome = "Teste Integração",
            documento = doc,
            endereco = EnderecoRequest("20040020", "100", null),
            unidadesConsumidoras = listOf(
                UnidadeConsumidoraRequest("UC1", "INT${System.nanoTime()}", EnderecoRequest(ucCep, "50", null))
            )
        )

    @Test
    fun `login retorna access token`() {
        assert(token.isNotBlank()) { "Token não deve ser vazio" }
    }

    @Test
    fun `rota protegida sem token retorna 401`() {
        val response = restTemplate.getForEntity("/api/v1/clientes", String::class.java)
        assert(response.statusCode == HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `criar cliente retorna 201 ou 202`() {
        val request = criarClienteRequest()
        val entity = HttpEntity(request, authHeaders())
        val response = restTemplate.exchange("/api/v1/clientes", HttpMethod.POST, entity, Map::class.java)
        assert(response.statusCode == HttpStatus.CREATED || response.statusCode == HttpStatus.ACCEPTED) {
            "Esperado 201 ou 202, recebeu ${response.statusCode}"
        }
    }

    @Test
    fun `listar clientes retorna 200`() {
        val entity = HttpEntity<Void>(authHeaders())
        val response = restTemplate.exchange("/api/v1/clientes", HttpMethod.GET, entity, Map::class.java)
        assert(response.statusCode == HttpStatus.OK)
        assert(response.body?.containsKey("content") == true)
    }

    @Test
    fun `documento invalido retorna 400`() {
        val request = criarClienteRequest(doc = "12345")
        val entity = HttpEntity(request, authHeaders())
        val response = restTemplate.exchange("/api/v1/clientes", HttpMethod.POST, entity, Map::class.java)
        assert(response.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `listar com ordenacao por data retorna 200`() {
        val entity = HttpEntity<Void>(authHeaders())
        val response = restTemplate.exchange("/api/v1/clientes?sort=createdAt,desc&size=20", HttpMethod.GET, entity, Map::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun `health retorna 200 sem token`() {
        val response = restTemplate.getForEntity("/actuator/health", Map::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }

    @Test
    fun `viacep status retorna 200 sem token`() {
        val response = restTemplate.getForEntity("/api/v1/integracoes/viacep/status", Map::class.java)
        assert(response.statusCode == HttpStatus.OK)
    }
}
