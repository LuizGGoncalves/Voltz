package com.treinamento.clientes.service

import com.treinamento.clientes.domain.model.Cliente
import com.treinamento.clientes.domain.model.Endereco
import com.treinamento.clientes.domain.model.UnidadeConsumidora
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.exception.DocumentoDuplicadoException
import com.treinamento.clientes.exception.DocumentoInvalidoException
import com.treinamento.clientes.exception.UfBloqueadaException
import com.treinamento.clientes.integration.viacep.ViaCepService
import com.treinamento.clientes.repository.ClienteRepository
import com.treinamento.clientes.repository.UnidadeConsumidoraRepository
import com.treinamento.clientes.web.dto.ClienteRequest
import com.treinamento.clientes.web.dto.EnderecoRequest
import com.treinamento.clientes.web.dto.UnidadeConsumidoraRequest
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class ClienteServiceTest {

    private val clienteRepository = mockk<ClienteRepository>()
    private val ucRepository = mockk<UnidadeConsumidoraRepository>()
    private val viaCepService = mockk<ViaCepService>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private lateinit var service: ClienteService

    @BeforeEach
    fun setup() {
        service = ClienteService(clienteRepository, ucRepository, viaCepService, eventPublisher)
    }

    private fun enderecoMg() = Endereco("30140071", "Rua dos Aimorés", "1", null, "Boa Viagem", "Belo Horizonte", "MG")
    private fun enderecoSp() = Endereco("01001000", "Praça da Sé", "1", null, "Sé", "São Paulo", "SP")
    private fun enderecoRj() = Endereco("20040020", "Av. Rio Branco", "1", null, "Centro", "Rio de Janeiro", "RJ")

    private fun criarRequest(endUc: String = "30140071") = ClienteRequest(
        nome = "Teste",
        documento = "39053344705",
        endereco = EnderecoRequest("30140071", "1", null),
        unidadesConsumidoras = listOf(
            UnidadeConsumidoraRequest("UC1", "INST001", EnderecoRequest(endUc, "1", null))
        )
    )

    @Test
    fun `documento invalido lanca excecao`() {
        val request = criarRequest().copy(documento = "12345")
        assertThrows<DocumentoInvalidoException> { service.criar(request) }
    }

    @Test
    fun `documento duplicado lanca excecao`() {
        every { clienteRepository.existsByDocumentoAndAtivoTrue("39053344705") } returns true
        assertThrows<DocumentoDuplicadoException> { service.criar(criarRequest()) }
    }

    @Test
    fun `finalizarCadastro bloqueia UC em SP`() {
        val cliente = Cliente().apply {
            nome = "Teste"
            documento = Documento.of("39053344705")
            endereco = enderecoMg()
            unidadesConsumidoras = mutableListOf(
                UnidadeConsumidora().apply {
                    nome = "UC SP"
                    numeroInstalacao = "SP001"
                    endereco = enderecoSp()
                    this.cliente = this@apply
                }
            )
        }
        assertThrows<UfBloqueadaException> { service.finalizarCadastro(cliente) }
    }

    @Test
    fun `finalizarCadastro bloqueia UC em RS`() {
        val cliente = Cliente().apply {
            nome = "Teste"
            documento = Documento.of("39053344705")
            endereco = enderecoMg()
            unidadesConsumidoras = mutableListOf(
                UnidadeConsumidora().apply {
                    nome = "UC RS"
                    numeroInstalacao = "RS001"
                    endereco = Endereco("90010000", "Rua", "1", null, "Centro", "Porto Alegre", "RS")
                    this.cliente = this@apply
                }
            )
        }
        assertThrows<UfBloqueadaException> { service.finalizarCadastro(cliente) }
    }

    @Test
    fun `finalizarCadastro permite UF nao bloqueada e publica evento MG`() {
        val cliente = Cliente().apply {
            nome = "Teste"
            documento = Documento.of("39053344705")
            endereco = enderecoMg()
            unidadesConsumidoras = mutableListOf(
                UnidadeConsumidora().apply {
                    nome = "UC MG"
                    numeroInstalacao = "MG001"
                    endereco = enderecoMg()
                    this.cliente = this@apply
                }
            )
        }

        every { clienteRepository.save(any()) } answers {
            (firstArg<Cliente>()).apply { id = 1L; unidadesConsumidoras.forEach { it.id = 1L } }
        }

        val result = service.finalizarCadastro(cliente)
        assertNotNull(result.id)
        verify { eventPublisher.publishEvent(any()) }
    }

    @Test
    fun `finalizarCadastro nao publica evento para UF diferente de MG`() {
        val cliente = Cliente().apply {
            nome = "Teste"
            documento = Documento.of("39053344705")
            endereco = enderecoRj()
            unidadesConsumidoras = mutableListOf(
                UnidadeConsumidora().apply {
                    nome = "UC RJ"
                    numeroInstalacao = "RJ001"
                    endereco = enderecoRj()
                    this.cliente = this@apply
                }
            )
        }

        every { clienteRepository.save(any()) } answers {
            (firstArg<Cliente>()).apply { id = 1L; unidadesConsumidoras.forEach { it.id = 1L } }
        }

        service.finalizarCadastro(cliente)
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }
}
