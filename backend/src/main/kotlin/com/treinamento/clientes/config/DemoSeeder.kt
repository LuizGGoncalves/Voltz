package com.treinamento.clientes.config

import com.treinamento.clientes.domain.model.*
import com.treinamento.clientes.domain.vo.Documento
import com.treinamento.clientes.repository.*
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Seeder de dados demo — popula o banco com clientes, UCs, pendentes e análises MG.
 *
 * Ativado APENAS quando `app.demo-seed=true` (desligado por padrão).
 * No docker-compose, passe: APP_DEMO_SEED=true
 *
 * Idempotente: só roda se não existem clientes cadastrados.
 */
@Component
@ConditionalOnProperty(name = ["app.demo-seed"], havingValue = "true")
class DemoSeeder(
    private val clienteRepository: ClienteRepository,
    private val ucRepository: UnidadeConsumidoraRepository,
    private val cadastroPendenteRepository: CadastroPendenteRepository,
    private val analiseClienteMgRepository: AnaliseClienteMgRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (clienteRepository.count() > 0) {
            log.info("Demo seed: banco já possui dados, pulando.")
            return
        }

        log.info("Demo seed: populando banco com dados de demonstração...")

        val clientes = criarClientes()
        val ucs = criarUCs(clientes)
        criarPendentes()
        criarAnalisesMg(clientes, ucs)

        log.info("Demo seed: concluído — {} clientes, {} UCs, pendentes e análises MG criados.",
            clientes.size, ucs.size)
    }

    private fun criarClientes(): List<Cliente> {
        val dados = listOf(
            Triple("Maria Clara Oliveira", "52998224725", endereco("30130000", "Av. Afonso Pena", "1500", "Sala 301", "Centro", "Belo Horizonte", "MG")),
            Triple("João Pedro Santos", "11144477735", endereco("20040020", "Av. Rio Branco", "156", "12º andar", "Centro", "Rio de Janeiro", "RJ")),
            Triple("Ana Beatriz Ferreira", "98765432000187", endereco("01310100", "Av. Paulista", "1000", "Conjunto 51", "Bela Vista", "São Paulo", "SP")),
            Triple("Carlos Eduardo Lima", "45612378901", endereco("30140071", "Rua da Bahia", "1148", null, "Centro", "Belo Horizonte", "MG")),
            Triple("Fernanda Costa Silva", "78945612300", endereco("40020000", "Rua Chile", "31", null, "Comércio", "Salvador", "BA")),
            Triple("Roberto Almeida Souza", "32165498700", endereco("70040010", "SBS Quadra 2", "15", "Bloco E", "Asa Sul", "Brasília", "DF")),
            Triple("Juliana Mendes Rocha", "65432198700", endereco("30120060", "Rua dos Caetés", "530", null, "Centro", "Belo Horizonte", "MG")),
            Triple("TechPower Energia Ltda", "12345678000195", endereco("22041080", "Rua Voluntários da Pátria", "45", "Galpão 3", "Botafogo", "Rio de Janeiro", "RJ")),
            Triple("Lucas Gabriel Martins", "95135745600", endereco("60060440", "Rua Major Facundo", "500", null, "Centro", "Fortaleza", "CE")),
            Triple("Patrícia Duarte Neves", "85274196300", endereco("30130005", "Rua dos Tupinambás", "169", "Apt 801", "Centro", "Belo Horizonte", "MG")),
        )

        return dados.map { (nome, doc, end) ->
            clienteRepository.save(
                Cliente(
                    nome = nome,
                    documento = Documento.fromDatabase(doc),
                    endereco = end
                )
            )
        }.also {
            // Inativar um cliente para demo do toggle
            val ana = it[2]
            ana.ativo = false
            clienteRepository.save(ana)
        }
    }

    private fun criarUCs(clientes: List<Cliente>): List<UnidadeConsumidora> {
        val ucsData = listOf(
            // Maria Clara (MG) — 2 UCs
            Triple(clientes[0], "Sede BH Centro" to "UC-MG-001", endereco("30130000", "Av. Afonso Pena", "1500", "Loja 1", "Centro", "Belo Horizonte", "MG")),
            Triple(clientes[0], "Filial Savassi" to "UC-MG-002", endereco("30140071", "Rua Pernambuco", "1000", null, "Savassi", "Belo Horizonte", "MG")),

            // João Pedro (RJ) — 1 UC
            Triple(clientes[1], "Escritório Centro RJ" to "UC-RJ-001", endereco("20040020", "Av. Rio Branco", "156", null, "Centro", "Rio de Janeiro", "RJ")),

            // Carlos Eduardo (MG) — 1 UC
            Triple(clientes[3], "Unidade Bahia" to "UC-MG-003", endereco("30140071", "Rua da Bahia", "1148", null, "Centro", "Belo Horizonte", "MG")),

            // Fernanda (BA) — 2 UCs
            Triple(clientes[4], "Loja Comércio" to "UC-BA-001", endereco("40020000", "Rua Chile", "31", null, "Comércio", "Salvador", "BA")),
            Triple(clientes[4], "Depósito Lauro de Freitas" to "UC-BA-002", endereco("42700000", "Rua da Harmonia", "200", "Galpão B", "Centro", "Lauro de Freitas", "BA")),

            // Roberto (DF) — 1 UC
            Triple(clientes[5], "Sede Brasília" to "UC-DF-001", endereco("70040010", "SBS Quadra 2", "15", "Bloco E", "Asa Sul", "Brasília", "DF")),

            // Juliana (MG) — 1 UC
            Triple(clientes[6], "Ateliê Centro" to "UC-MG-004", endereco("30120060", "Rua dos Caetés", "530", null, "Centro", "Belo Horizonte", "MG")),

            // TechPower (RJ) — 3 UCs
            Triple(clientes[7], "Datacenter Botafogo" to "UC-RJ-002", endereco("22041080", "Rua Voluntários da Pátria", "45", "Galpão 3", "Botafogo", "Rio de Janeiro", "RJ")),
            Triple(clientes[7], "Escritório Barra" to "UC-RJ-003", endereco("22631004", "Av. das Américas", "3500", "Sala 220", "Barra da Tijuca", "Rio de Janeiro", "RJ")),
            Triple(clientes[7], "Laboratório Niterói" to "UC-RJ-004", endereco("24020096", "Rua da Conceição", "188", null, "Centro", "Niterói", "RJ")),

            // Lucas (CE) — 1 UC
            Triple(clientes[8], "Filial Fortaleza" to "UC-CE-001", endereco("60060440", "Rua Major Facundo", "500", null, "Centro", "Fortaleza", "CE")),

            // Patrícia (MG) — 1 UC
            Triple(clientes[9], "Consultório Centro" to "UC-MG-005", endereco("30130005", "Rua dos Tupinambás", "169", "Sala 302", "Centro", "Belo Horizonte", "MG")),
        )

        return ucsData.map { (cliente, nomeInstalacao, end) ->
            val uc = UnidadeConsumidora(
                nome = nomeInstalacao.first,
                numeroInstalacao = nomeInstalacao.second,
                endereco = end,
                cliente = cliente
            )
            ucRepository.save(uc)
        }
    }

    private fun criarPendentes() {
        val pendentes = listOf(
            CadastroPendente(
                documento = "11122233344",
                payload = """{"nome":"Pedro Henrique Nascimento","documento":"111.222.333-44","endereco":{"cep":"01001000","numero":"100"},"unidadesConsumidoras":[{"nome":"UC Teste","numeroInstalacao":"UC-PND-001","endereco":{"cep":"01001000","numero":"50"}}]}""",
                status = "PENDENTE",
                tentativas = 2,
                ultimaTentativa = Instant.now().minusSeconds(300)
            ),
            CadastroPendente(
                documento = "55566677788",
                payload = """{"nome":"Larissa Freitas Barbosa","documento":"555.666.777-88","endereco":{"cep":"80010000","numero":"250"},"unidadesConsumidoras":[{"nome":"UC Curitiba","numeroInstalacao":"UC-PND-002","endereco":{"cep":"80010000","numero":"250"}}]}""",
                status = "REJEITADO",
                motivo = "Unidade consumidora 'UC Curitiba' em PR não é permitida.",
                tentativas = 1,
                ultimaTentativa = Instant.now().minusSeconds(7200)
            ),
            CadastroPendente(
                documento = "99988877766",
                payload = """{"nome":"Marcos Vinícius Teixeira","documento":"999.888.777-66","endereco":{"cep":"04101300","numero":"80"},"unidadesConsumidoras":[{"nome":"UC SP Centro","numeroInstalacao":"UC-PND-003","endereco":{"cep":"04101300","numero":"80"}}]}""",
                status = "REJEITADO",
                motivo = "Unidade consumidora 'UC SP Centro' em SP não é permitida.",
                tentativas = 1,
                ultimaTentativa = Instant.now().minusSeconds(3600)
            ),
            CadastroPendente(
                documento = "22233344455",
                payload = """{"nome":"Camila Rodrigues","documento":"222.333.444-55","endereco":{"cep":"30130000","numero":"42"},"unidadesConsumidoras":[{"nome":"UC BH","numeroInstalacao":"UC-PND-004","endereco":{"cep":"30130000","numero":"42"}}]}""",
                status = "PROCESSADO",
                tentativas = 3,
                ultimaTentativa = Instant.now().minusSeconds(600)
            ),
            CadastroPendente(
                documento = "88877766655",
                payload = """{"nome":"Ricardo Moura","documento":"888.777.666-55","endereco":{"cep":"99999999","numero":"1"},"unidadesConsumidoras":[{"nome":"UC Falha","numeroInstalacao":"UC-PND-005","endereco":{"cep":"99999999","numero":"1"}}]}""",
                status = "FALHA",
                motivo = "TTL expirado (24h)",
                tentativas = 5,
                ultimaTentativa = Instant.now().minusSeconds(86400)
            ),
        )

        pendentes.forEach { cadastroPendenteRepository.save(it) }
    }

    private fun criarAnalisesMg(clientes: List<Cliente>, ucs: List<UnidadeConsumidora>) {
        // UCs em MG: índices 0, 1 (Maria Clara), 3 (Carlos Eduardo), 7 (Juliana), 12 (Patrícia)
        val ucsMg = listOf(
            clientes[0] to ucs[0],  // Maria Clara — Sede BH
            clientes[0] to ucs[1],  // Maria Clara — Filial Savassi
            clientes[3] to ucs[3],  // Carlos Eduardo — Unidade Bahia (endereço MG)
            clientes[6] to ucs[7],  // Juliana — Ateliê Centro
            clientes[9] to ucs[12], // Patrícia — Consultório Centro
        )

        ucsMg.forEach { (cliente, uc) ->
            analiseClienteMgRepository.save(
                AnaliseClienteMg(
                    clienteId = requireNotNull(cliente.id),
                    unidadeConsumidoraId = requireNotNull(uc.id),
                    status = "PENDENTE_ANALISE"
                )
            )
        }
    }

    private fun endereco(
        cep: String, logradouro: String, numero: String,
        complemento: String?, bairro: String, cidade: String, uf: String
    ) = Endereco(
        cep = cep, logradouro = logradouro, numero = numero,
        complemento = complemento, bairro = bairro, cidade = cidade, uf = uf
    )
}
