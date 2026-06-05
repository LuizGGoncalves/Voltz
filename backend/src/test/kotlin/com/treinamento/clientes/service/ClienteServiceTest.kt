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
    private val auditoriaRepository = mockk<com.treinamento.clientes.repository.AuditoriaDocumentoRepository>(relaxed = true)

    private lateinit var service: ClienteService

    @BeforeEach
    fun setup() {
        service = ClienteService(clienteRepository, ucRepository, viaCepService, eventPublisher, auditoriaRepository)
    }

    private fun enderecoMg() = Endereco("30140071", "Rua dos Aimorés", "1", null, "Boa Viagem", "Belo Horizonte", "MG")
    private fun enderecoSp() = Endereco("01001000", "Praça da Sé", "1", null, "Sé", "São Paulo", "SP")
    private fun enderecoRj() = Endereco("20040020", "Av. Rio Branco", "1", null, "Centro", "Rio de Janeiro", "RJ")

    private fun criarCliente(ucEndereco: Endereco, ucNome: String = "UC", ucInstalacao: String = "INST001"): Cliente {
        val cliente = Cliente()
        cliente.nome = "Teste"
        cliente.documento = Documento.of("39053344705")
        cliente.endereco = enderecoMg()

        val uc = UnidadeConsumidora()
        uc.nome = ucNome
        uc.numeroInstalacao = ucInstalacao
        uc.endereco = ucEndereco
        uc.cliente = cliente
        cliente.unidadesConsumidoras = mutableListOf(uc)

        return cliente
    }

    @Test
    fun `documento invalido lanca excecao`() {
        val request = ClienteRequest(
            nome = "Teste", documento = "12345",
            endereco = EnderecoRequest("30140071", "1", null),
            unidadesConsumidoras = listOf(UnidadeConsumidoraRequest("UC", "I1", EnderecoRequest("30140071", "1", null)))
        )
        assertThrows<DocumentoInvalidoException> { service.criar(request) }
    }

    @Test
    fun `documento duplicado lanca excecao`() {
        every { clienteRepository.existsByDocumentoAndAtivoTrue("39053344705") } returns true
        val request = ClienteRequest(
            nome = "Teste", documento = "39053344705",
            endereco = EnderecoRequest("30140071", "1", null),
            unidadesConsumidoras = listOf(UnidadeConsumidoraRequest("UC", "I1", EnderecoRequest("30140071", "1", null)))
        )
        assertThrows<DocumentoDuplicadoException> { service.criar(request) }
    }

    @Test
    fun `finalizarCadastro bloqueia UC em SP`() {
        val cliente = criarCliente(enderecoSp(), "UC SP", "SP001")
        assertThrows<UfBloqueadaException> { service.finalizarCadastro(cliente) }
    }

    @Test
    fun `finalizarCadastro bloqueia UC em RS`() {
        val enderecoRs = Endereco("90010000", "Rua", "1", null, "Centro", "Porto Alegre", "RS")
        val cliente = criarCliente(enderecoRs, "UC RS", "RS001")
        assertThrows<UfBloqueadaException> { service.finalizarCadastro(cliente) }
    }

    @Test
    fun `finalizarCadastro bloqueia UC em PR`() {
        val enderecoPr = Endereco("80010000", "Rua", "1", null, "Centro", "Curitiba", "PR")
        val cliente = criarCliente(enderecoPr, "UC PR", "PR001")
        assertThrows<UfBloqueadaException> { service.finalizarCadastro(cliente) }
    }

    @Test
    fun `finalizarCadastro permite MG e publica evento`() {
        val cliente = criarCliente(enderecoMg(), "UC MG", "MG001")
        every { clienteRepository.save(any()) } answers {
            firstArg<Cliente>().apply { id = 1L; unidadesConsumidoras.forEach { it.id = 1L } }
        }

        val result = service.finalizarCadastro(cliente)
        assertNotNull(result.id)
        verify { eventPublisher.publishEvent(any<Any>()) }
    }

    @Test
    fun `finalizarCadastro nao publica evento para UF diferente de MG`() {
        val cliente = criarCliente(enderecoRj(), "UC RJ", "RJ001")
        every { clienteRepository.save(any()) } answers {
            firstArg<Cliente>().apply { id = 1L; unidadesConsumidoras.forEach { it.id = 1L } }
        }

        service.finalizarCadastro(cliente)
        verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
    }
}
