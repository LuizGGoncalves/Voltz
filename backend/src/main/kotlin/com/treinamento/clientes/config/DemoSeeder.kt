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
        criarPendentes(clientes)
        criarAnalisesMg(clientes, ucs)

        log.info("Demo seed: concluído — {} clientes, {} UCs criados.", clientes.size, ucs.size)
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
            Triple("Solar MG Distribuidora Ltda", "45678912000100", endereco("30180010", "Av. Amazonas", "812", "5º andar", "Centro", "Belo Horizonte", "MG")),
            Triple("Ricardo Gonçalves Pereira", "14725836900", endereco("20040030", "Rua Buenos Aires", "68", null, "Centro", "Rio de Janeiro", "RJ")),
            Triple("Camila Rodrigues Andrade", "36985214700", endereco("30130001", "Rua Espírito Santo", "605", "Sala 12", "Centro", "Belo Horizonte", "MG")),
            Triple("Marcos Vinícius Teixeira", "25836914700", endereco("40060200", "Av. Sete de Setembro", "240", null, "Vitória", "Salvador", "BA")),
            Triple("Energia Verde SA", "78912345000166", endereco("30140072", "Rua Guajajaras", "910", "Térreo", "Centro", "Belo Horizonte", "MG")),
        )

        return dados.mapIndexed { i, (nome, doc, end) ->
            clienteRepository.save(
                Cliente(nome = nome, documento = Documento.fromDatabase(doc), endereco = end)
            )
        }.also {
            // Inativar alguns clientes para demo
            it[2].ativo = false // Ana Beatriz (SP)
            clienteRepository.save(it[2])
            it[11].ativo = false // Ricardo (RJ)
            clienteRepository.save(it[11])
        }
    }

    private fun criarUCs(clientes: List<Cliente>): List<UnidadeConsumidora> {
        val ucsData = listOf(
            // Maria Clara (MG) — 2 UCs
            Triple(clientes[0], "Sede BH Centro" to "UC-MG-001", endereco("30130000", "Av. Afonso Pena", "1500", "Loja 1", "Centro", "Belo Horizonte", "MG")),
            Triple(clientes[0], "Filial Savassi" to "UC-MG-002", endereco("30140071", "Rua Pernambuco", "1000", null, "Savassi", "Belo Horizonte", "MG")),
            // João Pedro (RJ)
            Triple(clientes[1], "Escritório Centro RJ" to "UC-RJ-001", endereco("20040020", "Av. Rio Branco", "156", null, "Centro", "Rio de Janeiro", "RJ")),
            // Carlos Eduardo (MG)
            Triple(clientes[3], "Unidade Bahia" to "UC-MG-003", endereco("30140071", "Rua da Bahia", "1148", null, "Centro", "Belo Horizonte", "MG")),
            // Fernanda (BA) — 2 UCs
            Triple(clientes[4], "Loja Comércio" to "UC-BA-001", endereco("40020000", "Rua Chile", "31", null, "Comércio", "Salvador", "BA")),
            Triple(clientes[4], "Depósito Lauro de Freitas" to "UC-BA-002", endereco("42700000", "Rua da Harmonia", "200", "Galpão B", "Centro", "Lauro de Freitas", "BA")),
            // Roberto (DF)
            Triple(clientes[5], "Sede Brasília" to "UC-DF-001", endereco("70040010", "SBS Quadra 2", "15", "Bloco E", "Asa Sul", "Brasília", "DF")),
            // Juliana (MG)
            Triple(clientes[6], "Ateliê Centro" to "UC-MG-004", endereco("30120060", "Rua dos Caetés", "530", null, "Centro", "Belo Horizonte", "MG")),
            // TechPower (RJ) — 3 UCs
            Triple(clientes[7], "Datacenter Botafogo" to "UC-RJ-002", endereco("22041080", "Rua Voluntários da Pátria", "45", "Galpão 3", "Botafogo", "Rio de Janeiro", "RJ")),
            Triple(clientes[7], "Escritório Barra" to "UC-RJ-003", endereco("22631004", "Av. das Américas", "3500", "Sala 220", "Barra da Tijuca", "Rio de Janeiro", "RJ")),
            Triple(clientes[7], "Laboratório Niterói" to "UC-RJ-004", endereco("24020096", "Rua da Conceição", "188", null, "Centro", "Niterói", "RJ")),
            // Lucas (CE)
            Triple(clientes[8], "Filial Fortaleza" to "UC-CE-001", endereco("60060440", "Rua Major Facundo", "500", null, "Centro", "Fortaleza", "CE")),
            // Patrícia (MG)
            Triple(clientes[9], "Consultório Centro" to "UC-MG-005", endereco("30130005", "Rua dos Tupinambás", "169", "Sala 302", "Centro", "Belo Horizonte", "MG")),
            // Solar MG (MG) — 2 UCs
            Triple(clientes[10], "Sede Amazonas" to "UC-MG-006", endereco("30180010", "Av. Amazonas", "812", null, "Centro", "Belo Horizonte", "MG")),
            Triple(clientes[10], "Galpão Contagem" to "UC-MG-007", endereco("32010000", "Av. João César de Oliveira", "2000", null, "Eldorado", "Contagem", "MG")),
            // Camila (MG)
            Triple(clientes[12], "Escritório Espírito Santo" to "UC-MG-008", endereco("30130001", "Rua Espírito Santo", "605", "Sala 12", "Centro", "Belo Horizonte", "MG")),
            // Marcos (BA)
            Triple(clientes[13], "Loja Vitória" to "UC-BA-003", endereco("40060200", "Av. Sete de Setembro", "240", null, "Vitória", "Salvador", "BA")),
            // Energia Verde (MG) — 2 UCs
            Triple(clientes[14], "Usina Solar BH" to "UC-MG-009", endereco("30140072", "Rua Guajajaras", "910", null, "Centro", "Belo Horizonte", "MG")),
            Triple(clientes[14], "Subestação Betim" to "UC-MG-010", endereco("32600000", "Av. Edmeia Mattos Lazzarotti", "3001", null, "Centro", "Betim", "MG")),
        )

        return ucsData.map { (cliente, nomeInstalacao, end) ->
            ucRepository.save(UnidadeConsumidora(
                nome = nomeInstalacao.first,
                numeroInstalacao = nomeInstalacao.second,
                endereco = end,
                cliente = cliente
            ))
        }
    }

    private fun criarPendentes(clientes: List<Cliente>) {
        val now = Instant.now()

        // PROCESSADO com clienteId — para testar o link de navegação
        val processados = listOf(
            pend("22233344455", "Camila Rodrigues", "30130000", "UC-P-001", "PROCESSADO", 3, now.minusSeconds(600), clientes[12].id),
            pend("33344455566", "Fernando Souza Pinto", "20040020", "UC-P-002", "PROCESSADO", 1, now.minusSeconds(3600), clientes[1].id),
            pend("44455566677", "Gabriela Lima Castro", "30140071", "UC-P-003", "PROCESSADO", 2, now.minusSeconds(1800), clientes[3].id),
            pend("66677788899", "Renata Vieira Santos", "30130005", "UC-P-004", "PROCESSADO", 1, now.minusSeconds(900), clientes[9].id),
            pend("77788899900", "Bruno Carvalho Dias", "40020000", "UC-P-005", "PROCESSADO", 4, now.minusSeconds(7200), clientes[4].id),
            pend("10120230345", "Helena Moura Tavares", "30120060", "UC-P-006", "PROCESSADO", 1, now.minusSeconds(1200), clientes[6].id),
            pend("20230340456", "Diego Fonseca Lima", "60060440", "UC-P-007", "PROCESSADO", 2, now.minusSeconds(2400), clientes[8].id),
        )

        // REJEITADO (UF bloqueada)
        val rejeitados = listOf(
            rejeitado("55566677788", "Larissa Freitas Barbosa", "80010000", "UC Curitiba", "UC-R-001", "PR"),
            rejeitado("99988877766", "Marcos V. Teixeira Jr", "04101300", "UC SP Centro", "UC-R-002", "SP"),
            rejeitado("12312312312", "André Luis Meireles", "01310100", "UC Paulista", "UC-R-003", "SP"),
            rejeitado("32132132100", "Tatiana Borges", "80060000", "UC Curitiba Norte", "UC-R-004", "PR"),
            rejeitado("45645645600", "Cristiano Mendes", "90010000", "UC Porto Alegre", "UC-R-005", "RS"),
            rejeitado("65465465400", "Amanda Rezende", "01001000", "UC Sé", "UC-R-006", "SP"),
            rejeitado("78978978900", "Douglas Oliveira", "80020000", "UC Alto da XV", "UC-R-007", "PR"),
            rejeitado("98798798700", "Isabela Franco", "90020000", "UC Moinhos", "UC-R-008", "RS"),
            rejeitado("14714714700", "Leandro Batista", "80030000", "UC Batel", "UC-R-009", "PR"),
            rejeitado("25825825800", "Vanessa Moreira", "04301000", "UC Ipiranga", "UC-R-010", "SP"),
        )

        // PENDENTE (aguardando ViaCEP)
        val pendentes = listOf(
            pend("11122233344", "Pedro Henrique Nascimento", "01001000", "UC-PND-001", "PENDENTE", 2, now.minusSeconds(300), null),
            pend("22244466688", "Aline Cardoso", "01001000", "UC-PND-002", "PENDENTE", 1, now.minusSeconds(120), null),
            pend("33366699911", "Rafael Monteiro", "01001000", "UC-PND-003", "PENDENTE", 0, null, null),
            pend("44488800022", "Simone Aguiar", "01001000", "UC-PND-004", "PENDENTE", 3, now.minusSeconds(900), null),
            pend("55500011133", "Thiago Ribeiro", "01001000", "UC-PND-005", "PENDENTE", 1, now.minusSeconds(60), null),
            pend("66611122244", "Priscila Moura", "01001000", "UC-PND-006", "PENDENTE", 4, now.minusSeconds(1800), null),
            pend("77722233355", "Vinícius Costa", "01001000", "UC-PND-007", "PENDENTE", 0, null, null),
            pend("88833344466", "Elaine Duarte", "01001000", "UC-PND-008", "PENDENTE", 2, now.minusSeconds(600), null),
        )

        // FALHA (TTL expirado ou max tentativas)
        val falhas = listOf(
            falha("88877766655", "Ricardo Moura Silva", "99999999", "UC-F-001", "TTL expirado (24h)", 5),
            falha("99966655544", "Claudia Neves", "88888888", "UC-F-002", "Máximo de tentativas atingido (ViaCEP indisponível)", 5),
            falha("10011022033", "Paulo César Ramos", "77777777", "UC-F-003", "TTL expirado (24h)", 4),
            falha("20022033044", "Sandra Melo", "66666666", "UC-F-004", "CEP não encontrado: 66666666", 3),
            falha("30033044055", "Augusto Ferreira", "55555555", "UC-F-005", "Máximo de tentativas atingido (ViaCEP indisponível)", 5),
            falha("40044055066", "Márcia Lopes", "44444444", "UC-F-006", "TTL expirado (24h)", 5),
            falha("50055066077", "Jorge Henrique", "33333333", "UC-F-007", "CEP não encontrado: 33333333", 2),
        )

        val todos = processados + rejeitados + pendentes + falhas
        todos.forEach { cadastroPendenteRepository.save(it) }

        log.info("Demo seed: {} cadastros pendentes criados", todos.size)
    }

    private fun criarAnalisesMg(clientes: List<Cliente>, ucs: List<UnidadeConsumidora>) {
        val ucsMg = ucs.filter { it.endereco.uf == "MG" }
        ucsMg.forEach { uc ->
            analiseClienteMgRepository.save(
                AnaliseClienteMg(
                    clienteId = requireNotNull(uc.cliente?.id),
                    unidadeConsumidoraId = requireNotNull(uc.id),
                    status = "PENDENTE_ANALISE"
                )
            )
        }
        log.info("Demo seed: {} análises MG criadas", ucsMg.size)
    }

    // --- Helpers ---

    private fun pend(doc: String, nome: String, cep: String, ucInstalacao: String,
                     status: String, tentativas: Int, ultimaTentativa: Instant?, clienteId: Long?): CadastroPendente {
        val payload = """{"nome":"$nome","documento":"$doc","endereco":{"cep":"$cep","numero":"100"},"unidadesConsumidoras":[{"nome":"UC $nome","numeroInstalacao":"$ucInstalacao","endereco":{"cep":"$cep","numero":"50"}}]}"""
        return CadastroPendente(
            documento = doc, payload = payload, status = status,
            tentativas = tentativas, ultimaTentativa = ultimaTentativa, clienteId = clienteId
        )
    }

    private fun rejeitado(doc: String, nome: String, cep: String, ucNome: String, ucInstalacao: String, uf: String): CadastroPendente {
        val payload = """{"nome":"$nome","documento":"$doc","endereco":{"cep":"$cep","numero":"100"},"unidadesConsumidoras":[{"nome":"$ucNome","numeroInstalacao":"$ucInstalacao","endereco":{"cep":"$cep","numero":"50"}}]}"""
        return CadastroPendente(
            documento = doc, payload = payload, status = "REJEITADO",
            motivo = "Unidade consumidora '$ucNome' em $uf não é permitida.",
            tentativas = 1, ultimaTentativa = Instant.now().minusSeconds(7200)
        )
    }

    private fun falha(doc: String, nome: String, cep: String, ucInstalacao: String, motivo: String, tentativas: Int): CadastroPendente {
        val payload = """{"nome":"$nome","documento":"$doc","endereco":{"cep":"$cep","numero":"1"},"unidadesConsumidoras":[{"nome":"UC Falha","numeroInstalacao":"$ucInstalacao","endereco":{"cep":"$cep","numero":"1"}}]}"""
        return CadastroPendente(
            documento = doc, payload = payload, status = "FALHA",
            motivo = motivo, tentativas = tentativas, ultimaTentativa = Instant.now().minusSeconds(86400)
        )
    }

    private fun endereco(cep: String, logradouro: String, numero: String,
                         complemento: String?, bairro: String, cidade: String, uf: String) =
        Endereco(cep = cep, logradouro = logradouro, numero = numero,
            complemento = complemento, bairro = bairro, cidade = cidade, uf = uf)
}
