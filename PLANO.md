# Planejamento — Aplicação Fullstack de Gestão de Clientes (Spring Boot + Angular)

> Base: *Guia Prático de Treinamento Fullstack (Java/Kotlin + Angular)*
> Status: **PLANEJAMENTO COMPLETO** — nenhum código escrito ainda, nenhum commit. Especificação pronta para implementação.

---

## 0. Como usar este documento (handoff — leia primeiro)

**O que é:** especificação **completa e autossuficiente** para construir, do zero, uma aplicação fullstack de **gestão de clientes e unidades consumidoras**. Todas as decisões de arquitetura já foram tomadas e justificadas aqui. Você (dev ou agente) **não precisa de contexto externo** — tudo o que é necessário está neste arquivo.

**Como executar:**
1. Leia a **seção 1** (visão geral) e a **seção 2** (decisões — o "porquê" de cada escolha).
2. Siga o **Planejamento de sprints (seção 10)** **na ordem** (Sprint 0 → 6). Cada sprint tem objetivo, entregáveis, *Definition of Done* e riscos a vigiar.
3. Consulte sob demanda: **modelo de dados (4)**, **regras de negócio (5)**, **endpoints + exemplos JSON (6)**, **auditoria de riscos (8.1)** e os **apêndices** (A: validação; **C: dependências, config e esqueletos prontos**).
4. Respeite a **regra de ouro (8.2):** não inicie um bloco antes de sua dependência estar funcional.

**Ambiente alvo (host):** **dev 100% Docker** — só **Docker Desktop** + `git` no host; nenhum JDK/Node/Maven instalado na máquina (tudo roda em container — ver seção 2.2). Pasta do projeto: monorepo (`/backend`, `/frontend`, compose na raiz).

**Stack (resumo):** Kotlin + Spring Boot 3.x (JDK 21) · Maven · Spring Data JPA/Hibernate · PostgreSQL · Flyway · Spring Security (JWT/Resource Server) · springdoc-OpenAPI · **Angular standalone (≥17)** + Angular Material + ngx-mask. Versões exatas: pinar a **estável madura** vigente no momento do scaffold (política na seção 2).

**Convenções inegociáveis (não viole sem registrar):**
- Entidades JPA = **classes normais** (nunca `data class`); `data class` só em DTOs.
- DTO em todas as bordas (nunca expor entidade).
- Migrations **Flyway** versionadas e **imutáveis** após aplicadas; Hibernate em `ddl-auto: validate`.
- Erros em **RFC 7807 `ProblemDetail`**.
- Regra de UF (bloqueio/evento) só a partir de **UF enriquecida pelo ViaCEP no backend** (nunca confiar no cliente).
- `finalizarCadastro()` é a **fonte única** das regras de UF (usada pelo fluxo síncrono e pelo job de retry).

---

## 1. Visão geral do produto

Aplicação Fullstack que expõe uma API REST para **gestão de clientes** e suas **unidades consumidoras (UC)**, com um frontend Angular para operar todo o ciclo (cadastrar, atualizar, listar, consultar, inativar). Foco em organização, separação de responsabilidades e boas práticas de arquitetura.

Um **Cliente** possui **N Unidades Consumidoras** (relação 1:N confirmada pela "inclusão dinâmica de unidades consumidoras" no cadastro).

---

## 2. Decisões (confirmadas) e pendências

### Confirmadas
| # | Tema | Decisão |
|---|------|---------|
| D1 | Linguagem backend | **Kotlin** + Spring Boot |
| D2 | Mensageria do evento `analise_cliente_mg` | **Spring `ApplicationEvent`** (in-process) |
| D3 | Banco de dados | **PostgreSQL** via **Docker** desde o início |
| D4 | Escopo do MVP | **Tudo incluído**: Swagger/OpenAPI, testes (unit + integração), Angular Material + máscaras, **Docker** e **Spring Security** |
| D5 | Endereço | **Estruturado** via `@Embeddable Endereco` (`cep, logradouro, numero, complemento, bairro, cidade, uf`), reusado em Cliente e UC, com **UF obrigatória**. Detalhes na **seção 2.3** |
| D6 | Documento | **CPF e CNPJ** (tipo derivado do tamanho), validação **L2 (dígitos verificadores)**, **modelo híbrido**: Value Object `Documento` (invariante no domínio) + anotação `@Documento` na borda que **delega** ao VO + **groups** para validações contextuais por endpoint. Armazenado **normalizado (só dígitos)** + `UNIQUE`. Detalhes na **seção 2.4** e no **Apêndice A** |
| D7 | Migrations | **Flyway** (SQL versionado no Git) + Hibernate `ddl-auto: validate`. `V1__init.sql` gerado como rascunho pelo Hibernate e **revisado** antes de virar migration; depois trava em `validate`. Detalhes e motivação na **seção 2.1** |
| D8 | Spring Security | **JWT stateless (N3)**, validação via **OAuth2 Resource Server** (`oauth2ResourceServer().jwt()`), usuários no **banco + BCrypt + roles** (ADMIN/USER), **com refresh token revogável** (persistido no banco). Armazenamento OWASP: **access em memória + refresh em cookie `httpOnly`**. Detalhes na **seção 2.5** |

> As regras de bloqueio (SP/RS/PR) e evento (MG) operam sobre a **UF do endereço da UC**, por isso o endereço precisa ser estruturado (D5).

### Stack obrigatória do enunciado — conformidade (auditada)
O enunciado define como **obrigatórias** (não opcionais): **Java 17+ ou Kotlin · Spring Boot · JPA/Hibernate · Maven · Angular 14+**. Auditoria das nossas decisões:

| Obrigatório | Nossa decisão | Conformidade |
|-------------|---------------|--------------|
| Java 17+ **ou** Kotlin | Kotlin (D1) sobre **JDK 21** (D10) | ✅ "ou Kotlin" permitido; 21 ≥ 17 |
| Spring Boot | Spring Boot | ✅ |
| JPA / Hibernate | Spring Data JPA; Hibernate em `validate` (D7) | ✅ Hibernate segue como ORM |
| Maven | Maven (D9) | ✅ exato |
| Angular 14+ | Angular 17+ standalone (D24) | ✅ 17 ≥ 14 |

**Sem conflitos.** Tecnologias extras (Docker, PostgreSQL, Flyway, Spring Security, Swagger, testes, Angular Material, máscaras) são **aditivas** — várias estão na seção "Sugestões de Evolução" do PDF. Nota: **Flyway** não substitui JPA/Hibernate (cuida só do DDL/versionamento); o Hibernate permanece como ORM, mantendo conformidade com o requisito.

### Política de versões — estáveis e maduras (decidida)
Regra para **todas** as dependências do projeto:
- **Nunca** usar a versão `.0` recém-lançada de um major novo (risco de bugs e incompatibilidade de libs).
- **Sempre** uma versão **estável e madura**, no último *patch* dela.
- JVM: usar **LTS** → **JDK 21** (LTS maduro).
- Spring Boot: último **patch estável** da linha 3.x (sem milestone/RC/snapshot).
- Angular: major **estável consolidado** (standalone já padrão), não o recém-lançado.
- **Pin das versões exatas no momento do scaffold** (dentro do container), confirmando a estável vigente na data — sem adotar o major mais novo "fresco".

### D13 — Soft delete: `@SQLDelete` + filtro explícito (decidida)
**Decisão:** exclusão lógica via flag `ativo`, com três escolhas combinadas:
- **Remoção:** `@SQLDelete` na entidade → `repository.delete()` executa `UPDATE ... SET ativo = false` (sem boilerplate; mantém o dado no banco).
- **Visibilidade:** **sem `@SQLRestriction` global**. A listagem retorna **só ativos por padrão** e expõe um **toggle/parâmetro** (ex.: `?incluirInativos=true`) para incluir inativos — assim a coluna "Status ativo/inativo" do enunciado tem sentido.
- **Unicidade:** **índice único parcial** `UNIQUE(documento) WHERE ativo` e `UNIQUE(numero_instalacao) WHERE ativo` → documento/instalação de um registro removido pode ser recadastrado.

**Por que não usar `@SQLRestriction("ativo = true")` global (a pegadinha):** ele esconderia inativos de **todas** as consultas, o que **quebraria** o requisito de exibir "Status inativo" na listagem. Por isso o filtro de "só ativos" é **explícito e seletivo**, não automático global.

**Regras de implementação:**
- A checagem de duplicidade no service (`existsByDocumento...AndAtivoTrue`) deve filtrar `ativo = true`, para **casar** com o índice parcial (evita divergência service × banco).
- Repositório com métodos explícitos: `findAllByAtivoTrue(...)` (default) e `findAll(...)`/`findAllByAtivoFalse(...)` (toggle).
- **GET por ID** de um inativo retorna normalmente (com status), coerente com a listagem poder exibi-lo.
- Decisão derivada (D-extra): "últimos 20" considera **só ativos** por padrão (alinhado à listagem default).

### D20 + D21 — Integração ViaCEP (decidida)
**Decisão:** ViaCEP consultado nos **dois lados**, com responsabilidades distintas; backend é a **fonte da verdade** da UF.

- **Frontend (UX):** chama o ViaCEP direto (tem CORS) para **autopreencher** o formulário enquanto o usuário digita o CEP — o "autopreenchimento opcional" do PDF.
- **Backend (verdade):** ao salvar, **re-consulta** o ViaCEP e **enriquece/sobrescreve** `logradouro/bairro/cidade/uf` com a resposta; mantém só `numero`/`complemento` do cliente. Princípio **"never trust the client"** — a UF que gatilha as regras (bloqueio SP/RS/PR, evento MG) **não** pode vir confiável do cliente.

**Fallback e erros:**
| Situação | Tratamento |
|----------|------------|
| CEP não existe (`erro: true`) | `422` — CEP inválido |
| Latência | timeout curto (~3s) para não travar o request |
| ViaCEP **fora do ar / timeout** | **Fila persistente + retry** (ver abaixo) — não perde a tentativa e nunca confia na UF do cliente |

**Front (lock):** os campos vindos do CEP ficam **read-only**; o usuário **vê o CEP digitado** para conferência; o front valida e impede edição/adulteração. É camada de **UX** — a verdade continua sendo o backend (que enriquece/sobrescreve). Nota técnica: usar `readonly`/controle com `{value, disabled:true}` no Reactive Form para o valor ainda ir no submit.

### Resiliência: fila de cadastro pendente + retry (decidida)
Quando o ViaCEP está indisponível **no momento de salvar**, em vez de degradar (confiar na UF do cliente) ou rejeitar, o cadastro **não é perdido**: vai para uma **fila durável** e é enriquecido depois, de forma autoritativa.

**Design (limpo):** o cliente **só vira linha em `cliente` quando totalmente enriquecido**. Enquanto pendente, fica numa tabela separada `cadastro_pendente` — assim a tabela `cliente` nunca tem registros "pela metade".

```
Fluxo quando ViaCEP está DOWN no submit:
1. Backend grava o payload bruto em cadastro_pendente (status=PENDENTE) e responde 202 Accepted + id.
2. Front mostra "cadastro em processamento" e pode consultar GET /api/v1/cadastros-pendentes/{id}.
3. Job @Scheduled (com backoff) reprocessa os PENDENTE:
   - ViaCEP OK + UF permitida  → cria Cliente (ativo) · status=PROCESSADO · (se MG → evento analise_cliente_mg)
   - ViaCEP OK + UF SP/RS/PR    → status=REJEITADO (motivo: UF bloqueada) — NÃO cria Cliente
   - ViaCEP ainda down          → incrementa tentativas, mantém PENDENTE
   - Estouro de tentativas      → status=FALHA (requer ação/aviso)
```

**Por que tabela separada e não um status no Cliente:** as regras (bloqueio SP/RS/PR, evento MG) dependem da UF, que ainda não temos enquanto pendente. Manter o pendente fora da tabela `cliente` evita registros em estado indeterminado e mantém a listagem/consultas limpas.

**Tensão registrada (consciente):** um cadastro pode ser **aceito (202) e depois rejeitado** pelo job se a UF revelar SP/RS/PR. Isso é inerente ao enriquecimento assíncrono e **precisa ser indicado no front** (badge/estado "rejeitado: UF bloqueada").

**Mecanismo:** **tabela `cadastro_pendente` no Postgres + `@Scheduled`** (sem broker — reusa o banco, fica dentro da stack/Docker; coerente com D2 que evitou mensageria externa). Esta fila é **distinta** do evento `analise_cliente_mg` (D2) — propósitos diferentes.

#### Fechando os furos da fila (correções de design — decididas)
A fila é a **feature de destaque** (com tela de acompanhamento no front). Para ela ser robusta, três correções:

**1. Duplicidade (documento/instalação) — defesa em 3 camadas:**
- **Submit:** checa o documento contra **clientes ativos E a fila** (`status=PENDENTE`) → `409` imediato.
- **Banco (à prova de corrida):** índice único parcial na fila `UNIQUE(documento) WHERE status='PENDENTE'` — impede dois pendentes simultâneos com o mesmo doc.
- **Criação (rede final):** ao criar o `Cliente`, o índice parcial de `cliente` (`WHERE ativo`) é o árbitro; duplicado → `REJEITADO (documento já cadastrado)`.
- Requer **coluna `documento`** na `cadastro_pendente` (alimenta índice + checagem). Mesmo padrão para `numero_instalacao`.

**2. "Aceita e depois rejeita" — minimizar o adiamento:**
- Tudo que **não depende do ViaCEP** é validado **síncrono no submit** (documento válido/único, instalação única, campos, formato de CEP). Só o **bloqueio de UF (SP/RS/PR) e o evento MG** podem ser adiados (dependem da UF).
- O **front aplica o bloqueio de UF na hora** (já tem o endereço do autofill) → usuário honesto não submete UF bloqueada.
- Rejeição tardia só sobra quando **ViaCEP do front E do back falharam** (raríssimo), e fica **contextualizada pelo badge** (D34). 

**3. Regra duplicada nos 2 caminhos — fonte única:**
- Caminho síncrono e job **convergem** para `finalizarCadastro(payload, endereco)`, onde vivem a **regra de UF (bloqueio + evento MG)** e a criação — **uma vez só**. A única diferença entre os caminhos é quem chama o ViaCEP. Elimina divergência.
```
cadastrar(payload): validarSemViaCep(payload); enriquecer? finalizarCadastro(...) : enfileirar(...)
retryJob(pendente):  enriquecer? finalizarCadastro(...) : incrementaTentativa(...)
finalizarCadastro(payload, endereco):  // FONTE ÚNICA
    aplicarRegraUF(uf) // SP/RS/PR + evento MG
    revalidarUnicidade(); criarCliente()
```

**Técnico geral:**
- Cliente HTTP: **`RestClient`** (Spring 6) com timeout.
- **UF sempre obrigatória e validada** no backend (D5).
- Cache de CEP e endpoint proxy (`GET /api/v1/enderecos/{cep}`): opcionais/evolução. No MVP, frontend chama o ViaCEP direto; backend chama ao salvar.
- Política de retry: backoff + máximo de tentativas → `FALHA` com aviso.

### Indicador de status do ViaCEP (D34 — decidida)
**Contexto:** o ViaCEP **não tem health endpoint, nem SLA**, e a doc avisa que *"uso massivo pode bloquear o acesso por tempo indeterminado"*. Logo, **não** dá para "pingar o ViaCEP" direto — temos que sondar com parcimônia.

**Decisão:** health probe **próprio** no backend, exposto ao front:
- **`ViaCepHealthIndicator`** (Spring Boot Actuator) faz uma consulta **leve** a um CEP conhecido (ex.: `01001000`) com timeout curto.
- **Sonda agendada + cache (30–60s)** → uma única verificação no backend; **nunca** por request. Protege contra o bloqueio por uso massivo.
- Exposto em `/actuator/health` (component) e/ou `GET /api/integracoes/viacep/status`.
- **Front lê do nosso backend** (não do ViaCEP) → 1 sonda central, não N navegadores pingando.

**Front (UX + transparência):** badge de status com explicação do fluxo:
- 🟢 *disponível* → autofill normal.
- 🟡 *indisponível* → "Você ainda pode cadastrar; o endereço será validado quando o serviço voltar. O cadastro ficará 'em processamento' e não será perdido." (deixa o fluxo assíncrono/`202` **claro**, evitando confusão).

**Caveats:** badge é **informativo, nunca bloqueante** (cadastro não depende dele); risco de falso-negativo por timeout pontual; conceitualmente separado do health do **nosso** app (banco etc.), é um *component* de dependência externa no Actuator.

**Requisito de UX (front):** o status precisa ser **exibido na tela de forma clara e agradável** para o usuário verificar — não só um texto solto. Padrão esperado: indicador visual (badge/chip com cor + ícone), tooltip/área explicando o funcionamento (autofill quando online; fila + "em processamento" quando offline), e feedback consistente (Angular Material + snackbars). A entrega do front deve ser **polida** (boa UX, responsiva, acessível), não apenas funcional.

**Parâmetros da fila de retry — a finalizar (parametrizável via `.env`/config):**
| Parâmetro | Default proposto | Observação |
|-----------|------------------|------------|
| Máximo de tentativas | **5** | depois → `FALHA` + aviso |
| Estratégia de intervalo | **backoff exponencial** (1, 2, 4, 8, 16 min) | evita martelar o ViaCEP |
| Frequência do job `@Scheduled` | a cada **1 min** | varre os `PENDENTE` elegíveis |
| TTL/expiração do pendente | **24h** | após isso, `FALHA` definitiva |
> Valores a confirmar quando detalharmos a Fase 2; todos configuráveis sem recompilar.

### D14 — Relação Cliente↔UC (decidida)
**Mapeamento:** bidirecional — **UC é dona da FK** (`@ManyToOne` + coluna `cliente_id`); Cliente tem `@OneToMany(mappedBy = "cliente", cascade = ALL, orphanRemoval = true, fetch = LAZY)`.

| Dimensão | Decisão | Por quê |
|----------|---------|---------|
| Direção/FK | Bidirecional, UC dona | eficiente (sem tabela de junção) e navegável (`cliente.unidades`) |
| Cascade | **ALL** | cobre inclusão dinâmica (PERSIST/MERGE) **e** propaga a remoção |
| Soft delete da UC | **UC também com `@SQLDelete`** | remover o cliente (soft) cascateia **soft delete** nas UCs — consistente, sem hard delete; UCs preservadas para auditoria como inativas |
| orphanRemoval | **true** | tirar uma UC do formulário na edição remove (vira soft delete, pois a UC tem `@SQLDelete`) |
| Fetch | **LAZY** | listagem não carrega UCs à toa; detalhe/edição usa fetch join/`@EntityGraph` |

**Consequências:**
- UC ganha coluna **`ativo`** (boolean) + `@SQLDelete` — alinhado ao índice único parcial `WHERE ativo` do `numero_instalacao` (D13).
- Tanto cascade-remove (apagar cliente) quanto orphanRemoval (tirar UC na edição) resultam em **soft delete** da UC (graças ao `@SQLDelete` da UC).
- Montar DTO dentro da transação ou com fetch join para evitar `LazyInitializationException`.

### D11 — Estrutura de repositório: Monorepo (decidida)
**Decisão:** backend (Spring Boot) e frontend (Angular) no **mesmo repositório Git**, em pastas separadas (`/backend`, `/frontend`), com os arquivos de orquestração (compose) na raiz.

**Motivação:**
- **Commits/PRs atômicos:** uma feature que toca back e front entra junta; versões sempre sincronizadas.
- **Velocidade e consistência:** um `docker compose up` na raiz sobe tudo; setup único; sem coordenar versões entre repos.
- **Dev assistido por IA:** a IA enxerga back+front no **mesmo contexto**, fazendo mudanças coordenadas e entendendo o sistema inteiro — motivo legítimo e moderno.

**Cuidados para a escolha se manter boa:**
- Manter **fronteiras limpas** entre `/backend` e `/frontend` (sem acoplamento de build) — preserva a opção de separar no futuro a custo baixo.
- Decisão **reversível**: extrair uma pasta para repo próprio (`git filter-repo`) preserva histórico, se um dia for necessário.

### Decisões secundárias (backlog D9–D33 — a tratar em ordem)
> Defaults propostos; serão confirmados/aprofundados na sequência. As de segurança (D28/D29/D32/D33) já foram **parcialmente resolvidas** pelo D8.

| # | Tema | Default proposto | Prioridade | Status |
|---|------|------------------|------------|--------|
| D9 | Build tool | **Maven** (item obrigatório da stack do enunciado) | 🔴 | ✅ decidido |
| D10 | JDK p/ Kotlin | **JDK 21 (LTS)** — satisfaz o piso "Java 17+" | 🟡 | ✅ decidido |
| D11 | Estrutura de repositório | **Monorepo** (`/backend`, `/frontend` no mesmo repo Git) | 🟡 | ✅ decidido |
| D12 | Estratégia de ID | **`Long` / IDENTITY (`BIGSERIAL`)** — idiomático, performático; risco de enumeration mitigado pela auth (D8) | 🟡 | ✅ decidido |
| D13 | Soft delete | **`@SQLDelete`** (delete→`UPDATE ativo=false`) · **sem `@SQLRestriction` global** · listagem mostra **só ativos por padrão + toggle** para inativos (atende a coluna "Status") · **índice único parcial** `WHERE ativo` em `documento`/`numeroInstalacao`. Detalhes na **seção 2.6** | 🔴 | ✅ decidido |
| D14 | Relação Cliente↔UC | **UC dona da FK** (`@ManyToOne`); Cliente `@OneToMany(mappedBy)` com **cascade `ALL` + `orphanRemoval=true` + fetch `LAZY`**. **UC também tem `@SQLDelete`** → remover o cliente (soft) cascateia **soft delete** nas UCs, e remover uma UC na edição também é soft (coerente com D13). Casa com "inclusão dinâmica de UCs". Atenção: montar DTO dentro da transação / fetch join p/ evitar `LazyInitializationException`. Detalhes na **seção 2.8** | 🟡 | ✅ decidido |
| D15 | Documento editável após criar? | **Imutável no fluxo do usuário** (PUT não expõe o documento). **Correção só por ADMIN** via endpoint dedicado `PATCH /clientes/{id}/documento` (`@PreAuthorize ADMIN`), com re-validação de unicidade + **auditoria** (quem/quando/motivo). Caso de suporte, sem troca de identidade pelo usuário comum | 🟢→🟡 | ✅ decidido |
| D16 | Versionamento da API | **`/api/v1/...`** (versão na URL) — evolui sem quebrar consumidores antigos; `v2` futura convive com `v1` | 🟢 | ✅ decidido |
| D17 | Paginação | **`Pageable` (Spring Data)** — `?page&size&sort`, retorna `Page<T>` com metadados; casa com o paginator do Angular Material. "Últimos 20" segue como query fixa à parte | 🟡 | ✅ decidido |
| D18 | Formato de erro | **RFC 7807 `ProblemDetail`** (nativo do Spring 6, habilitado por config) — `{type,title,status,detail,instance}` + extensão `errors[]` para validação de campos. Sem formato custom | 🟡 | ✅ decidido |
| D19 | Mapeamento DTO↔entidade | **Funções de extensão Kotlin** (manual): `Cliente.toResponse()`, `ClienteRequest.toEntity()`. Zero dependência, explícito; sem MapStruct (ganho marginal com poucos DTOs, evita plugin de anotação no build Kotlin) | 🟢 | ✅ decidido |
| D20 | Onde o ViaCEP é chamado | **Ambos (split)**: frontend p/ autofill (UX) + backend p/ validar/enriquecer (verdade). Detalhes na **seção 2.7** | 🔴 | ✅ decidido |
| D21 | UF: fonte/validação | **Enriquecer** (ViaCEP = fonte; sobrescreve uf/logradouro/bairro/cidade) · fallback = **fila persistente + retry** (`cadastro_pendente` + `@Scheduled`) se ViaCEP cair (não perde tentativa, não confia na UF do cliente) · UF sempre obrigatória/validada · front read-only. Detalhes na **seção 2.7** | 🔴 | ✅ decidido |
| D22 | Status do bloqueio SP/RS/PR | **`422 Unprocessable Entity`** + `ProblemDetail` com `detail` claro (ex.: "UC em SP não permitida"). Request válido, regra de negócio recusa. Distinto de `400` (formato) e `409` (duplicidade) | 🟡 | ✅ decidido |
| D23 | Ação do listener do evento MG | **Apenas enfileirar/registrar** numa lista própria `analise_cliente_mg` (sem processamento de negócio por ora — "o que fazer" fica para o futuro). Lista **visualizável** no front (`GET /api/v1/analises-mg`). Execução: **`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`** (evita análise fantasma em rollback; não bloqueia) | 🟡 | ✅ decidido |
| D24 | Versão do Angular | **Angular standalone** (major estável maduro, ≥17; satisfaz o piso "14+"). *Standalone* = componentes autocontidos com `imports` próprios, **sem NgModules** (padrão desde o 17, abordagem oficial atual). Versão exata fixada no scaffold (estável vigente, não a recém-lançada) | 🔴 | ✅ decidido |
| D25 | Gerência de estado | **RxJS (assíncrono: HTTP/eventos/debounce) + Signals (estado de UI: loading, listas, status ViaCEP, `computed`)**, complementares via `toSignal`/`toObservable`. **Sem NgRx** (overkill). Régua: "ao longo do tempo→RxJS, valor agora→Signal" | 🟡 | ✅ decidido |
| D26 | Biblioteca de máscara | ngx-mask | 🟢 | aberto |
| D27 | Cadastro/edição: página ou modal | **Páginas dedicadas** (rotas `/clientes/novo`, `/clientes/{id}/editar`) — form grande/dinâmico (UCs + endereços), URL própria (deep-link/refresh/voltar), bom em mobile. Modais só p/ confirmações pontuais | 🟢 | ✅ decidido |
| D28 | Onde ficam os usuários | Banco | 🟡 | ✅ resolvido pelo D8 |
| D29 | Rotas públicas × protegidas | Públicas: `auth/login`,`auth/refresh`, `actuator/health`(+viacep status). **Tudo o mais autenticado.** Criar/editar = **USER**; inativar (DELETE) e corrigir documento = **ADMIN**; **Swagger protegido**. Matriz na **seção 2.9** | 🟡 | ✅ decidido |
| D30 | Banco nos testes de integração | **Testcontainers (Postgres real, efêmero)** — mesmo banco de prod, container descartável por execução; Flyway monta o schema idêntico. Valida índice parcial `WHERE ativo`/tipos que o H2 mascararia. Coerente com a stack 100% Docker | 🟡 | ✅ decidido |
| D31 | CI (GitHub Actions) | **Pipeline de validação de PR (sem deploy)**: em PR/push p/ `main` roda **lint + testes** do back (Maven + Testcontainers + ktlint/detekt) e do front (ESLint + testes headless) + build. **Branch protection** na `main` (merge só com checks verdes). Sem deploy automático | 🟢→🟡 | ✅ decidido |
| D32 | Refresh token | Sim (access + refresh) | 🟡 | ✅ resolvido pelo D8 |
| D33 | Roles/authorities | Sim (ADMIN/USER) | 🟡 | ✅ resolvido pelo D8 |
| D34 | Indicador de status do ViaCEP | **No MVP**: `ViaCepHealthIndicator` (Actuator) com **sonda agendada + cache 30-60s** (evita bloqueio por uso massivo) + endpoint de status + badge no front com explicação do fluxo assíncrono. Detalhes na **seção 2.7** | 🟡 | ✅ decidido |

---

## 2.1 Estratégia de migrations — Flyway + Hibernate `validate` (decidida)

**Decisão:** o schema do banco é de responsabilidade do **Flyway** (scripts SQL versionados no Git). O Hibernate roda em `ddl-auto: validate` — **não cria nem altera nada**, apenas valida que as entidades batem com o schema no boot. A primeira migration (`V1__init.sql`) é **gerada como rascunho pelo Hibernate, revisada à mão e congelada**; a partir daí toda mudança é uma nova migration escrita manualmente.

### Como funciona (setup definitivo — vale para o projeto inteiro)
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate     # Hibernate só fiscaliza, não mexe no schema
  flyway:
    enabled: true            # Flyway cria/altera o schema via migrations
```
Fluxo no start do backend: **Flyway aplica migrations novas → Hibernate valida entidades × schema → app sobe (ou falha com erro claro se houver divergência)**.

### Como nasce o `V1__init.sql` (truque de partida — uma única vez)
1. Liga temporariamente o Hibernate para **gerar o DDL** das entidades (script de schema-generation).
2. Hibernate produz o `CREATE TABLE ...` de Cliente, UnidadeConsumidora e Endereco.
3. **Revisa o SQL** (nomes de constraints, índices, `UNIQUE` no documento, tipos do Postgres) e cola em `V1__init.sql`.
4. Desliga o truque e volta para `validate` + Flyway. Próximas mudanças = migrations novas (`V2`, `V3`, ...), escritas à mão.

### Convenções (disciplina obrigatória)
- **Migration aplicada é imutável**: nunca editar um script já rodado (quebra o checksum do Flyway). Correção vira migration nova.
- **Numeração combinada** entre devs para evitar colisão de versão em branches (ou versões por timestamp).
- O `validate` do Hibernate é a **rede de segurança** contra "mudei a entidade e esqueci a migration".

### Por que esta estratégia foi escolhida (motivação registrada)
- **Histórico e autoria reais** vêm do **Git** sobre os arquivos `.sql` (autor do commit, PR, revisão) — algo que o `ddl-auto` não oferece, pois não gera artefato nenhum.
- **Reprodutibilidade total no Docker**: apagar o volume do Postgres e subir de novo reconstrói o schema **idêntico** a partir dos scripts — coerente com o objetivo de ambiente 100% containerizado.
- **Seguro em produção e com múltiplos devs**: evita as alterações imprevisíveis do `ddl-auto: update` (que nunca remove colunas e depende do estado anterior do banco).
- **Combina com Testcontainers**: nos testes de integração, cada Postgres limpo recebe o mesmo schema de produção via Flyway.
- **Custo inicial mitigado**: o trabalho braçal do primeiro script é eliminado pela geração via Hibernate (rascunho revisado), mantendo o controle fino do SQL.
- **`validate` como guarda-corpo**: a divergência entidade × schema falha já no boot, com mensagem clara, em vez de explodir em runtime.

---

## 2.2 Estratégia de desenvolvimento — 100% Docker (sem nada instalado no host)

**Princípio:** o host só precisa de **um runtime de containers**. Nenhum JDK, Kotlin, Gradle/Maven, Node, npm ou Angular CLI é instalado na máquina — tudo roda dentro de containers, com o código-fonte montado por *volume* (edição no host, execução no container).

### Único pré-requisito no host
- **Runtime de containers**: **Docker Desktop** (decidido) — inclui Docker Engine + Compose. *Ainda não instalado na máquina; instalação será o primeiro passo da execução.*
- `git` (já presente).

> Status do host (verificado): `git` ✅ · Docker ❌ (a instalar) · Java/Node ❌ (não serão instalados — rodam só em container).

### Como cada tarefa roda sem instalar nada
| Tarefa | Como roda |
|--------|-----------|
| Criar projeto Spring/Kotlin | Container descartável (`maven:3-eclipse-temurin-21` ou Spring CLI em container) gera o scaffold |
| Criar projeto Angular | Container `node:20` rodando `ng new` (CLI via `npx`, sem instalar Node no host) |
| Rodar backend em dev | Container backend com Maven + Spring DevTools (**hot reload**), código por volume |
| Rodar frontend em dev | Container Node rodando `ng serve --host 0.0.0.0`, porta 4200, código por volume |
| Banco | Container PostgreSQL com volume nomeado para persistência |
| Build de produção | Dockerfiles **multi-stage** (compila e empacota dentro do container) |
| Comandos do dia a dia | `docker compose` + atalhos (`Makefile`/scripts `.ps1`) — ex.: `compose up`, `compose run backend ./mvnw test` |

### Arquivos de orquestração planejados
- `docker-compose.yml` — stack de **dev**: `db` (Postgres), `backend` (Maven dev + hot reload), `frontend` (ng serve).
- `docker-compose.prod.yml` — stack de **produção**: imagens finais multi-stage + Nginx servindo o Angular.
- `backend/Dockerfile` — multi-stage (build Kotlin → runtime JRE slim).
- `frontend/Dockerfile` — multi-stage (build Angular → Nginx).
- `.dockerignore`, `.env` (variáveis: portas, credenciais Postgres, JWT secret).
- `Makefile` / `scripts/` — atalhos para os comandos `docker compose run/exec`.

### Pontos de atenção do fluxo dockerizado
- **Hot reload**: backend via Spring DevTools + volume; frontend via `ng serve`.
- **node_modules / `.m2` (cache Maven) / `target`**: usar volumes nomeados ou anônimos para não poluir o host e manter performance.
- **Permissões/linha de fim de arquivo**: configurar `.gitattributes` para manter LF no repositório.

---

## 2.3 Estratégia de endereço — objeto estruturado `@Embeddable` (decidida)

**Decisão:** o endereço é modelado como um **objeto estruturado** (`Endereco`), com campos separados, e não como texto livre. Implementado como `@Embeddable` em JPA — uma classe sem tabela própria cujos campos viram **colunas na tabela do dono** (Cliente e UC). O mesmo `Endereco` é **reaproveitado** nas duas entidades (DRY).

```kotlin
@Embeddable
data class Endereco(
    val cep: String,
    val logradouro: String,
    val numero: String,
    val complemento: String?,   // opcional
    val bairro: String,
    val cidade: String,
    val uf: String,             // OBRIGATÓRIO — base das regras de negócio
)
```

### Por que estruturado (motivação registrada)
- **Regras de UF triviais e confiáveis:** o bloqueio de UC em **SP/RS/PR** e o evento para **MG** leem `endereco.uf` diretamente — `if (uc.endereco.uf in setOf("SP","RS","PR")) ...` — sem parsing nem adivinhação sobre texto.
- **Encaixe direto no ViaCEP:** a resposta da API (`logradouro`, `bairro`, `localidade`, `uf`...) mapeia 1:1 nos campos do objeto.
- **Autopreenchimento natural no Angular:** ao consultar o CEP, cada campo é preenchido individualmente; sobra ao usuário só número e complemento.
- **Consultável:** permite filtrar/relatar por cidade ou UF.
- **Validação por campo:** formato de CEP, UF com 2 letras, obrigatoriedade granular.

### Modelagem: `@Embeddable` (não entidade separada)
Endereço pertence ao seu dono (Cliente/UC), não é compartilhado nem tem ciclo de vida próprio → `@Embeddable` (colunas na própria tabela), evitando JOIN e tabela extra desnecessários.

### Regra crítica
- **UF é sempre obrigatória e validada**, independentemente do ViaCEP. Se a API estiver indisponível ou o CEP não existir, o usuário informa a UF manualmente — caso contrário as regras de bloqueio (SP/RS/PR) e o evento (MG) não teriam como rodar.

### Consequências em cascata
| Camada | Impacto |
|--------|---------|
| Entidades | `Endereco` `@Embeddable` reusado em Cliente e UC |
| Migration V1 | Colunas de endereço em `cliente` e `unidade_consumidora` |
| Regras de negócio | UF disponível direto → bloqueio SP/RS/PR e evento MG triviais |
| Integração ViaCEP | Mapeamento 1:1 da resposta no objeto |
| DTOs | Bloco de endereço aninhado em request/response |
| Frontend | Sub-formulário de endereço + autofill por CEP |
| Validação | Por campo (CEP, UF obrigatória) |

---

## 2.4 Estratégia de documento (CPF/CNPJ) — decidida (modelo híbrido)

**Decisão:** o cadastro aceita **CPF e CNPJ**, com validação de **dígitos verificadores (L2)**, implementada em um **modelo híbrido (Bean Validation + Value Object/DDD)**. O valor é **armazenado normalizado (só dígitos)**, com coluna `UNIQUE`; a máscara existe apenas na exibição (frontend).

### ⚠️ Ressalva consciente (registrada propositalmente)
O modelo híbrido é, reconhecidamente, **levemente over-engineered para o porte deste projeto** — um CRUD de treino funcionaria só com Bean Validation. A escolha é **deliberada** e se justifica por:
- **Qualidade adicional e proteção em camadas:** a invariante no domínio garante que um documento inválido **nunca circula em nenhuma camada**, não só quando o `@Valid` é acionado.
- **Demonstração de boas práticas de arquitetura** (critério explícito de autoavaliação do guia): separa claramente *invariante de domínio* de *validação contextual de borda*.
- **Extensibilidade real:** o cenário de "validação exclusiva por endpoint" (documento válido, mas que precisa atender a uma regra específica naquele ponto de entrada) fica naturalmente suportado via **groups**, sem contaminar o domínio.

> Em resumo: aceitamos um pouco mais de cerimônia hoje para ganhar integridade garantida, clareza arquitetural e um caminho de evolução limpo. É uma troca consciente em favor de qualidade.

### Arquitetura híbrida (as três peças e a fonte única da verdade)
1. **Value Object `Documento` (domínio) — a invariante.** Imutável, auto-validável: `Documento.of(entrada)` normaliza e valida (CPF/CNPJ + dígitos); se inválido, **lança exceção** e o objeto não nasce. É a **fonte única da verdade** do que é "um documento válido".
2. **Anotação `@Documento` (borda) — o filtro de entrada.** `ConstraintValidator` que **delega** para `Documento.ehValido(...)` (não reimplementa a regra → DRY). Acionada por `@Valid`/`@Validated` no DTO → erro **`400` agregado** com `fieldErrors`.
3. **Groups (borda) — validações contextuais por endpoint.** Interfaces-marcador (`OnCreate`, `OnImportacaoCorporativa`, ...) ativadas por `@Validated(Grupo::class)` no método do controller. Regras específicas de um contexto vivem **aqui, na borda — nunca dentro do VO** (senão o VO rejeitaria documentos válidos em outros fluxos).

Mapeamento JPA do VO: `AttributeConverter` (`Documento` ↔ `String` no banco). Ao **ler** do banco, o VO é reconstruído sem re-validação paranoica (o banco está dentro da fronteira confiável — *always-valid domain model*).

### Onde cada validação mora
| Responsabilidade | Local | Tipo | Erro |
|------------------|-------|------|------|
| "É um CPF/CNPJ válido" (dígitos) | **VO `Documento`** | invariante (universal) | exceção → traduzida no handler |
| Filtro de input na API | **`@Documento`** no DTO (delega ao VO) | validação de borda | `400` agregado |
| Regra específica de um endpoint | **groups** (`@Validated(Grupo)`) | validação contextual | `400` |
| Normalização (só dígitos) | dentro do `Documento.of()` | — | — |
| Unicidade (regra de negócio nº1) | `ClienteService` (consulta repo) | regra com I/O | `409 Conflict` |
| Última linha | `UNIQUE` na coluna (Flyway) | constraint de banco | tratada no handler |

### Regras de implementação
- **Tipo derivado do tamanho:** 11 díg → CPF; 14 → CNPJ (`TipoDocumento` exposto pelo VO; usado pela máscara no frontend).
- **Pegadinha de groups:** ao ativar um grupo não-`Default`, as constraints `Default` deixam de rodar — usar `@GroupSequence` quando precisar de ambos.
- **Kotlin:** usar use-site target `@field:Documento`; VO como `@JvmInline value class` (ou `data class` se múltiplos campos).
- **Algoritmo L2:** pode reaproveitar `@CPF`/`@CNPJ` do Hibernate Validator (`org.hibernate.validator.constraints.br`) **dentro** da lógica, mas a porta de entrada canônica é o VO.

### Consequências em cascata
| Camada | Impacto |
|--------|---------|
| Domínio | Value Object `Documento` + `TipoDocumento` |
| Entidade Cliente | campo `documento` (via `AttributeConverter`), `UNIQUE` |
| Migration V1 | `documento VARCHAR(14) NOT NULL UNIQUE` |
| DTO de entrada | anotação `@Documento` + estrutura de `groups` |
| Service | unicidade (`409`) |
| Handler global | traduz exceção de invariante e violação de `UNIQUE` em respostas claras |
| Frontend | máscara dinâmica CPF/CNPJ + validação visual |

> Ver **Apêndice A** para a fundamentação completa (invariante × validação contextual, camadas de defesa, mecânica de groups) com referências.

---

## 2.5 Estratégia de segurança (Spring Security) — decidida

**Decisão:** autenticação **JWT stateless (N3)**. O login emite um token assinado; o cliente o envia em `Authorization: Bearer ...`. A validação usa o **OAuth2 Resource Server** do Spring (`oauth2ResourceServer().jwt()`) — sem filtro custom. Usuários ficam **no banco**, senha com **BCrypt**, com **roles** (`ADMIN`/`USER`). Há **refresh token** (access curto + refresh longo).

### Por que assim (motivação registrada)
- **JWT stateless** é o padrão para SPA (Angular) + API REST: escala bem e não exige sessão no servidor.
- **OAuth2 Resource Server** é a forma idiomática no Spring Security 6: validação de assinatura/claims pronta, *"sem filtro custom nem boilerplate"*, mapeando claims→authorities via `JwtAuthenticationConverter`. Menos superfície de bug que um `OncePerRequestFilter` manual.
- **Usuários no banco + BCrypt + roles** demonstra um modelo de identidade real (critério de boas práticas do guia) e prepara autorização fina.
- **Refresh token** permite sessões longas sem reautenticar, mantendo o access token de vida curta (boa prática de segurança).

### Conceito: o que é "stateless" (e o que a doc recomenda)
**Stateless = o servidor não guarda nada sobre o cliente entre requisições.** Há dois modelos opostos de "lembrar que o usuário está logado":
- **Stateful (sessão):** o servidor cria um objeto de sessão na memória dele e devolve só um ID em cookie (`JSESSIONID`). A cada request ele **procura** quem é aquele ID. O servidor *lembra* — isso é estado.
- **Stateless (JWT):** o login devolve um **token assinado autocontido** (`{ sub, roles, exp }`). A cada request o servidor só **confere a assinatura e lê os claims** — não guarda nada. O estado de sessão fica **no cliente**.

Isso atende à **restrição de Statelessness do REST** (Fielding): *"cada requisição deve conter toda a informação necessária para ser entendida, sem se aproveitar de contexto armazenado no servidor."*

**Recomendação da documentação (Spring Security):** `SessionCreationPolicy.STATELESS` é *"particularly recommended for REST API implementations where statelessness is a core architectural requirement"*. Atenção: a doc **não** diz "stateless é sempre melhor" — para app web tradicional (páginas no servidor) o default `IF_REQUIRED` (sessão) é o correto. Stateless é o certo **para o nosso caso** (SPA Angular + API REST).

### Papéis OAuth2 e os "dois chapéus" do nosso app
O OAuth2 separa segurança em papéis; os dois que importam aqui:
- **Authorization Server** — autentica o usuário (login) e **emite** o token.
- **Resource Server** — **hospeda os recursos protegidos** (`/api/v1/clientes`), **recebe e valida** o token e libera o recurso se válido. **Não faz login.**

Analogia: o Authorization Server é a **recepção do hotel** (confere documento, entrega o cartão-chave = token); o Resource Server é a **fechadura da porta** (não sabe quem você é, só verifica se o cartão é válido e abre — sem ligar para a recepção a cada vez = stateless).

No nosso projeto, **um único app Spring veste os dois chapéus**:
- **Chapéu Authorization Server** (emitir): `AuthController` + `JwtService` (login → token).
- **Chapéu Resource Server** (validar): `oauth2ResourceServer().jwt()` protegendo os endpoints.

Por isso usar "Resource Server" faz sentido **mesmo sem IdP externo**: ele é o componente que valida o token e protege os recursos, independentemente de quem o emitiu. Se um dia adotarmos um IdP externo (Keycloak/Auth0), trocamos só o "emissor" (o decoder/`issuer-uri`) — o Resource Server continua igual.

### Resource Server vs filtro custom (por que Resource Server)
São as duas formas de validar o Bearer token:
- **Filtro custom (`OncePerRequestFilter`):** você escreve o parsing, a validação de assinatura/`exp`/`iss`, o tratamento de erro e o mapeamento claims→roles. Dá controle total (ex.: recarregar usuário do banco a cada request), mas é **boilerplate sensível à segurança** sob sua responsabilidade.
- **OAuth2 Resource Server (escolhido):** o framework já traz a cadeia pronta — `BearerTokenAuthenticationFilter` (extrai) → `JwtDecoder` (valida assinatura/claims) → `JwtAuthenticationConverter` (claims→authorities). Validamos **nosso próprio token** configurando um `JwtDecoder` com a chave (`NimbusJwtDecoder.withSecretKey(...)` HMAC ou `.withPublicKey(...)` RSA).

A doc é explícita: o suporte nativo é *"strongly recommended over custom filters... should be the default choice for production applications"* (motivos: validação completa pronta, rotação automática de chave, menos superfície de bug, extensível via converters). **Contrapartida consciente:** o Resource Server valida por assinatura/claims e **não recarrega o usuário do banco a cada request** → a revogação imediata depende do esquema de refresh token (abaixo) + access token curto.

### Armazenamento de token e revogação (prática OWASP) — decidido
Onde o token fica no cliente importa **mais** que stateless vs stateful. Seguindo OWASP (*"tokens must NOT be stored in localStorage/sessionStorage"* — qualquer XSS lê tudo lá):
- **Access token (curto, ~15 min)** → mantido **em memória** no JS (variável; some ao recarregar).
- **Refresh token (longo, ~7 dias)** → **cookie `httpOnly` + `Secure` + `SameSite`** (inacessível a JavaScript → imune a roubo por XSS).
- **Refresh token persistido no banco** → permite **revogação** (deslogar/invalidar antes de expirar), recuperando o controle que o stateless puro não tem.

> Honestidade arquitetural: isso torna o sistema **híbrido** — validação do access é stateless, mas o refresh tem um *pouco* de estado (a tabela de refresh tokens) para ser revogável. É o meio-termo pragmático e seguro adotado pelo mercado.

### Componentes
```
security/
├── SecurityConfig            # SecurityFilterChain (lambda DSL): STATELESS, CSRF off, CORS,
│                             #   regras de rota, oauth2ResourceServer().jwt()
├── JwtService                # emissão de access + refresh; JwtDecoder (NimbusJwtDecoder) p/ validar
├── JwtAuthenticationConverter# mapeia claims do JWT → roles/authorities
├── RefreshTokenService       # persiste/valida/revoga refresh tokens no banco
└── handlers                  # AuthenticationEntryPoint (401) + AccessDeniedHandler (403) em JSON
web/controller/AuthController # POST /auth/login → {access (corpo) + refresh (cookie httpOnly)}; /auth/refresh; /auth/logout
domain/model/                 # Usuario, Role (+ join usuario_role), RefreshToken
service/UsuarioDetailsService # UserDetailsService → carrega do banco
config/                       # PasswordEncoder (BCrypt)
```

### Configuração canônica (Spring Boot 3 / Security 6)
- `SecurityFilterChain` como **bean** (sem `WebSecurityConfigurerAdapter`, removido).
- `SessionCreationPolicy.STATELESS`.
- **CSRF desabilitado** (sem cookie de sessão → CSRF não agrega, só atrapalha).
- **CORS** liberado para o container do frontend.
- `@EnableMethodSecurity` para `@PreAuthorize` (autorização por role).
- **BCrypt** para senhas.

### Consequências em cascata
| Área | Impacto |
|------|---------|
| Flyway | tabelas `usuario`, `role`, `usuario_role`, `refresh_token` + seed de admin inicial |
| Rotas (D29) | ver matriz na seção 2.9 (tudo autenticado por padrão; USER cria/edita; ADMIN inativa/corrige documento; Swagger protegido) |
| Swagger | esquema `bearerAuth` (botão "Authorize" com token) |
| Frontend (D24+) | tela de login, **HTTP interceptor** injetando o access (memória), **route guards**, refresh via cookie `httpOnly` |
| Erros | `401` (sem/expirado) e `403` (sem permissão) padronizados em JSON |
| Testes | `@WithMockUser`/mock de auth nos testes de controller |

### Pendências derivadas
- **D29** — fechar a **lista exata** de rotas públicas × protegidas.
- Definir **tempo de expiração** do access (~15 min) e do refresh (~7 dias) — ajustável.
- Definir **rotação** do refresh token (rotacionar a cada uso vs. reutilizar até expirar) e formato da chave de assinatura (HMAC simétrico vs. par RSA).

---

## 2.9 Matriz de acesso (rotas públicas × protegidas — D29)

Princípio: **`anyRequest().authenticated()`** (tudo protegido por padrão) + lista branca explícita de públicas.

| Rota | Acesso |
|------|--------|
| `POST /api/v1/auth/login`, `/auth/refresh` | **Pública** |
| `POST /api/v1/auth/logout` | Autenticado |
| `GET /actuator/health` (+ `/api/v1/integracoes/viacep/status`) | **Pública** (health/badge) |
| `/actuator/**` (demais) | ADMIN |
| **`/swagger-ui/**`, `/v3/api-docs/**`** | **Protegido** (autenticado) |
| `POST /api/v1/clientes`, `PUT /api/v1/clientes/{id}` (criar/editar) | **USER** (qualquer autenticado) |
| `GET /api/v1/clientes/**` (listar/consultar/últimos), `/cadastros-pendentes`, `/analises-mg` | Autenticado |
| `DELETE /api/v1/clientes/{id}` (inativar) | **ADMIN** |
| `PATCH /api/v1/clientes/{id}/documento` (correção) | **ADMIN** |

Decisões: criar/editar liberado a **USER**; **inativar** e **corrigir documento** restritos a **ADMIN** (ações sensíveis); **Swagger protegido** (não público).

---

## 3. Arquitetura proposta

### 3.1 Backend (Kotlin + Spring Boot) — arquitetura em camadas

```
com.treinamento.clientes
├── ClientesApplication.kt   # @EnableJpaAuditing, @EnableScheduling, @EnableAsync, @EnableMethodSecurity
├── config/            # CORS, OpenAPI/Swagger, SecurityConfig, JpaAuditingConfig, AsyncConfig
├── domain/
│   ├── model/         # Cliente, UnidadeConsumidora, Endereco (@Embeddable), CadastroPendente,
│   │                  #   AnaliseClienteMg, Usuario, Role, RefreshToken
│   ├── vo/            # Documento (Value Object) + TipoDocumento
│   └── event/         # AnaliseClienteMgEvent
├── repository/        # *Repository (Spring Data JPA)
├── service/           # ClienteService (finalizarCadastro = fonte única), ViaCepService,
│   │                  #   CadastroPendenteService, RefreshTokenService, UsuarioDetailsService
│   ├── job/           # RetryCadastroJob (@Scheduled)
│   └── event/         # Listener do evento MG (@TransactionalEventListener(AFTER_COMMIT)+@Async)
├── web/
│   ├── controller/    # Cliente, Auth, CadastroPendente, AnaliseMg, IntegracaoViaCep
│   ├── dto/           # Request/Response (data classes Kotlin) — desacoplado da entidade
│   └── mapper/        # Funções de extensão Kotlin (manual) — toResponse()/toEntity() (D19)
├── security/          # SecurityConfig, JwtService, JwtAuthenticationConverter, handlers 401/403
│                      #   (validação via OAuth2 Resource Server — SEM filtro custom)
├── integration/viacep/# RestClient + ViaCepHealthIndicator (Actuator)
└── exception/         # Exceptions de negócio + @RestControllerAdvice (ProblemDetail/RFC 7807)
```

**Stack:** Kotlin, Spring Boot, Spring Web, Spring Data JPA, Bean Validation, Spring Security (JWT), springdoc-openapi, Flyway, PostgreSQL, **Maven**.

**Padrões:** `data class` para DTOs, DTO em todas as bordas (nunca expor entidade), validação com Bean Validation (`@Valid`), tratamento global de erros (`@RestControllerAdvice`) com payload padronizado, soft delete via flag `ativo`, migrations versionadas com Flyway.

### 3.2 Frontend (Angular standalone, ≥17 — D24)

```
src/app
├── core/              # services (Cliente, ViaCep, Auth), interceptors (auth/erro),
│                      #   guards (authGuard, adminGuard), models, app.config.ts (providers)
├── shared/            # componentes reutilizáveis, máscaras (ngx-mask), pipes, status-badge
├── features/
│   ├── auth/login/    # tela de login
│   ├── clientes/lista # listagem paginada (+toggle inativos/badges)
│   ├── clientes/form  # cadastro/edição standalone com UCs dinâmicas (FormArray)
│   ├── pendentes/     # acompanhamento da fila de cadastro (D20/D21)
│   └── analise-mg/    # lista dos casos do evento MG (D23)
└── app.routes.ts      # rotas standalone (sem NgModules)
```
*Componentes standalone (`imports` próprios), sem NgModules. Bootstrap via `bootstrapApplication` + `provideRouter`/`provideHttpClient`.*

**Padrões:** Reactive Forms com `FormArray` para UCs dinâmicas, validação reativa, feedback via snackbar/toast, separação service/componente, tipagem forte (interfaces).

**Estado (D25):** RxJS nas bordas (HTTP/eventos/debounce), **Signals** para estado de UI (`signal`/`computed`/`effect`), com `toSignal`/`toObservable` na ponte. Sem NgRx.

**Barra de qualidade de UX (requisito):** a entrega do front deve ser **polida** — visual agradável e consistente (Angular Material), **responsiva**, acessível, com estados claros de **loading/sucesso/erro/pendente**. Itens que precisam estar **visíveis na tela** para o usuário: status do ViaCEP (badge + explicação), estado "cadastro em processamento" (`202`/fila) e resultado, badges de status ativo/inativo na listagem, máscaras de CEP/documento e mensagens de validação amigáveis. Não basta funcionar; precisa comunicar bem o que está acontecendo.

---

## 4. Modelo de dados detalhado

### Cliente
| Campo | Tipo | Regras |
|-------|------|--------|
| id | Long (PK) | auto |
| nome | String | obrigatório |
| documento | String | obrigatório, CPF/CNPJ válido (VO), **único parcial** `WHERE ativo` |
| endereco | Endereco (embutido) | obrigatório |
| ativo | boolean | default `true`; soft delete = `false` |
| createdAt | timestamp (UTC) | `@CreatedDate` (auditoria automática) |
| updatedAt | timestamp (UTC) | `@LastModifiedDate` |
| version | Long | `@Version` — optimistic locking (evita lost update; risco #10) |
| unidadesConsumidoras | List\<UC\> | 1:N, cascade |

### Unidade Consumidora
| Campo | Tipo | Regras |
|-------|------|--------|
| id | Long (PK) | auto |
| nome | String | obrigatório |
| numeroInstalacao | String | obrigatório, **único parcial** `WHERE ativo` (não pode pertencer a 2 clientes ativos) |
| endereco | Endereco (embutido) | obrigatório; UF usada nas regras SP/RS/PR/MG |
| cliente | Cliente (FK) | **dona da relação** (`@ManyToOne`, coluna `cliente_id`) |
| ativo | boolean | soft delete (`@SQLDelete`); cascateia do cliente (D14) |

### Endereco (@Embeddable / reutilizado)
`cep, logradouro, numero, complemento, bairro, cidade, uf`

### Cadastro Pendente (fila de retry — D20/D21)
Tabela separada (o cliente só entra em `cliente` quando enriquecido).
| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long (PK) | auto |
| documento | String | **extraído** do payload → alimenta índice único parcial + checagem de duplicidade |
| payload | JSON/colunas | dados brutos do cadastro (incl. CEP, UCs) |
| status | enum | `PENDENTE` / `PROCESSADO` / `REJEITADO` / `FALHA` |
| motivo | String | ex.: "UF bloqueada (SP)" / "documento já cadastrado" |
| tentativas | int | contador p/ backoff/limite |
| createdAt / ultimaTentativa | timestamp | auditoria do retry |

> Índice: `UNIQUE(documento) WHERE status='PENDENTE'` (anti-duplicidade na fila). O `Cliente` só é criado quando o pendente é processado com sucesso.

### Análise Cliente MG (lista do evento `analise_cliente_mg` — D23)
Lista própria, **distinta** da `cadastro_pendente`. Só registra (sem processamento por ora); visualizável no front.
| Campo | Tipo | Observação |
|-------|------|------------|
| id | Long (PK) | auto |
| clienteId | FK | cliente que disparou |
| unidadeConsumidoraId | FK | UC em MG que gerou o evento |
| status | enum | `PENDENTE_ANALISE` (futuro: o que fazer com isso) |
| createdAt | timestamp | quando entrou na análise |

### Segurança (D8) — Usuario / Role / RefreshToken
| Entidade | Campos principais | Observação |
|----------|-------------------|------------|
| Usuario | id, username/email, senha (**BCrypt**), ativo, roles | seed de admin inicial |
| Role | id, nome (`ADMIN`/`USER`) | join `usuario_role` (N:N) |
| RefreshToken | id, usuarioId, tokenHash, expiraEm, revogado | persistido p/ **revogação** (D8) |

---

## 5. Regras de negócio → onde implementar

| Regra | Implementação planejada |
|-------|--------------------------|
| Documento não duplicado | Índice **único parcial** `WHERE ativo` + verificação no service (`existsByDocumentoAndAtivoTrue`) → `409 Conflict` |
| UC não pode pertencer a 2 clientes | `numeroInstalacao` **único parcial** `WHERE ativo` + validação no service ao salvar |
| Consultar endereço via ViaCEP | `ViaCepService` (RestClient): frontend autofill + backend enriquece/sobrescreve uf etc. (fonte da verdade); **fallback = fila persistente + retry** se ViaCEP cair (D20/D21) |
| Remoção lógica | `@SQLDelete` → `UPDATE ativo=false`; listagem mostra só ativos por padrão + toggle p/ inativos (D13) |
| Bloquear UC em SP/RS/PR | Em `finalizarCadastro()`: se `uf ∈ {SP, RS, PR}` → **`422`** + `ProblemDetail` com `detail` claro (D22) |
| Evento `analise_cliente_mg` (UF = MG) | Publicar `ApplicationEvent` em `finalizarCadastro()`; listener `@TransactionalEventListener(AFTER_COMMIT)`+`@Async` **só registra** na lista `analise_cliente_mg` (sem processamento — D23); visível em `GET /api/v1/analises-mg` |

---

## 6. Endpoints REST (contrato proposto)

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/v1/clientes` | Cadastrar cliente (com UCs) → `201` ou **`202 Accepted`** + id pendente se ViaCEP down (D20/D21) |
| PUT | `/api/v1/clientes/{id}` | Atualizar cliente (**não** altera o documento — D15) |
| PATCH | `/api/v1/clientes/{id}/documento` | **Correção do documento — só ADMIN** (`@PreAuthorize`); re-valida unicidade + auditoria (D15) |
| DELETE | `/api/v1/clientes/{id}` | Remoção lógica (`@SQLDelete` → `ativo=false`) — **só ADMIN** (D29) |
| GET | `/api/v1/clientes` | Listar **paginado** (`?page&size&sort`; só ativos por padrão; `?incluirInativos=true` toggle — D13/D17) → `Page<ClienteResumoDTO>` |
| GET | `/api/v1/clientes/{id}` | Consultar por ID |
| GET | `/api/v1/clientes/ultimos` | Últimos 20 ativos em ordem decrescente |
| GET | `/api/v1/cadastros-pendentes` | **Lista paginada** da fila (tela de acompanhamento; filtro por status) — D20/D21 |
| GET | `/api/v1/cadastros-pendentes/{id}` | Status de um item da fila (`PENDENTE`/`PROCESSADO`/`REJEITADO`/`FALHA`) |
| GET | `/api/v1/integracoes/viacep/status` | Status (cacheado) do ViaCEP p/ o badge no front (D34) |
| GET | `/api/v1/analises-mg` | Lista (paginada) dos casos do evento `analise_cliente_mg` p/ visualização (D23) |
| GET | `/api/v1/enderecos/{cep}` | Proxy ViaCEP (opcional/evolução; frontend chama direto no MVP) |
| POST | `/api/v1/auth/login` · `/refresh` · `/logout` | Autenticação (D8) — públicas (login/refresh) |

Respostas de erro: **RFC 7807 `ProblemDetail`** (D18) — `{ type, title, status, detail, instance }` + extensão `errors[]` (campo→mensagem) nas validações. `@RestControllerAdvice` personaliza só as exceções de negócio.

### 6.1 Exemplos de contrato (JSON)

**POST `/api/v1/clientes` — request** (documento e CEP com ou sem máscara; backend normaliza):
```json
{
  "nome": "Maria Silva",
  "documento": "390.533.447-05",
  "endereco": { "cep": "01001-000", "numero": "100", "complemento": "ap 12" },
  "unidadesConsumidoras": [
    { "nome": "Casa", "numeroInstalacao": "123456789", "endereco": { "cep": "30140-071", "numero": "50" } }
  ]
}
```
> Backend **enriquece** logradouro/bairro/cidade/uf via ViaCEP a partir do `cep` (ignora esses campos se vierem do cliente).

**201 Created — response** (ViaCEP disponível):
```json
{
  "id": 1, "nome": "Maria Silva", "documento": "39053344705", "tipoDocumento": "CPF",
  "ativo": true, "createdAt": "2026-06-04T12:00:00Z", "updatedAt": "2026-06-04T12:00:00Z",
  "endereco": { "cep": "01001000", "logradouro": "Praça da Sé", "numero": "100",
                "complemento": "ap 12", "bairro": "Sé", "cidade": "São Paulo", "uf": "SP" },
  "unidadesConsumidoras": [ { "id": 1, "nome": "Casa", "numeroInstalacao": "123456789",
    "endereco": { "cep": "30140071", "logradouro": "...", "cidade": "Belo Horizonte", "uf": "MG" } } ]
}
```

**202 Accepted — response** (ViaCEP fora do ar → entrou na fila):
```json
{ "cadastroPendenteId": 42, "status": "PENDENTE", "mensagem": "Cadastro em processamento; o endereço será validado automaticamente." }
```

**GET `/api/v1/clientes?page=0&size=20&sort=nome,asc` — response** (`Page`):
```json
{ "content": [ { "id": 1, "nome": "Maria Silva", "documento": "39053344705",
    "ativo": true, "createdAt": "...", "updatedAt": "..." } ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 20 }
```

**422 — bloqueio de UF (ProblemDetail):**
```json
{ "type": "about:blank", "title": "Unprocessable Entity", "status": 422,
  "detail": "Unidade consumidora em SP não é permitida.", "instance": "/api/v1/clientes" }
```

**400 — validação (ProblemDetail + `errors[]`):**
```json
{ "type": "about:blank", "title": "Bad Request", "status": 400,
  "detail": "Falha de validação", "instance": "/api/v1/clientes",
  "errors": [ { "field": "documento", "message": "Documento inválido (CPF ou CNPJ)" } ] }
```

**409 — documento duplicado:** `ProblemDetail` `status:409`, `detail:"Documento já cadastrado."`

**POST `/api/v1/auth/login` — request/response:**
```json
// request
{ "username": "admin", "password": "..." }
// 200 — access no corpo; refresh vai em cookie httpOnly (Set-Cookie)
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900 }
```

---

## 7. Backlog por fases

### Fase 0 — Setup & infraestrutura (100% Docker)
- [ ] **Pré-requisito**: instalar **Docker Desktop** no host (decidido)
- [ ] Definir estrutura de monorepo: `/backend`, `/frontend`, `/docker`, arquivos compose na raiz
- [ ] `.gitattributes` (CRLF→LF), `.gitignore`, `.dockerignore`, `.env.example`
- [ ] Gerar scaffold **Kotlin + Spring Boot** via container descartável (Web, JPA, Validation, Security, PostgreSQL Driver, Flyway, Actuator)
- [ ] Configurar **`kotlin-allopen` + `kotlin-noarg` (preset `jpa`)** no `pom.xml` (senão o Hibernate quebra com entidades Kotlin) · convenção: entidades = classes normais, `data class` só em DTOs
- [ ] Gerar scaffold **Angular** via container `node` (`ng new`) + Angular Material
- [ ] `docker-compose.yml` (dev): `db` (Postgres) + `backend` (hot reload) + `frontend` (ng serve)
- [ ] Configurar `application.yml` (datasource Postgres por env, JPA, Flyway)
- [ ] Migration inicial Flyway (schema clientes + UCs)
- [ ] `Makefile`/scripts `.ps1` com atalhos dos comandos compose
- [ ] Configurar CORS (frontend container → backend container)
- [ ] **Reverse proxy de dev** (proxy do `ng serve` ou nginx) para front+back na **mesma origem** → cookie `httpOnly` do refresh funciona (mitiga risco #5)
- [ ] Habilitar `@EnableJpaAuditing`, `@EnableScheduling`, `@EnableAsync`, `@EnableMethodSecurity`
- [ ] Validar que `docker compose up` sobe os 3 serviços e o backend conecta no Postgres

### Fase 1 — Backend núcleo (CRUD)
- [ ] Entidades JPA (Cliente, UC, Endereco) + auditoria (`@EnableJpaAuditing`)
- [ ] Repositories (Spring Data JPA)
- [ ] DTOs (request/response) + mappers
- [ ] ClienteService com CRUD + soft delete
- [ ] ClienteController + Bean Validation
- [ ] Handler global de exceções (`@RestControllerAdvice`)
- [ ] Endpoint "últimos 20 desc"

### Fase 2 — Regras de negócio
- [ ] Validação documento único (índice parcial `WHERE ativo` + service)
- [ ] Validação número de instalação único (UC não compartilhada; índice parcial)
- [ ] **`finalizarCadastro()` como fonte única** da regra de UF (bloqueio SP/RS/PR + evento MG) — chamado pelo caminho síncrono e pelo job
- [ ] Integração ViaCEP (`ViaCepService` com `RestClient` + timeout; enriquecer)
- [ ] Fila de retry: tabela `cadastro_pendente` (+ coluna `documento` + índice parcial `WHERE status=PENDENTE`) + job `@Scheduled` (backoff/limite) + `202 Accepted` + endpoint de status
- [ ] Dedup em 3 camadas (submit: clientes+fila · índice parcial · rede final na criação)
- [ ] `ViaCepHealthIndicator` (Actuator) com sonda agendada + cache 30-60s + endpoint `/api/integracoes/viacep/status`
- [ ] Evento `analise_cliente_mg`: ApplicationEvent publicado em `finalizarCadastro` + listener `@TransactionalEventListener(AFTER_COMMIT)`+`@Async` que **só registra** na tabela `analise_cliente_mg` + endpoint `GET /api/v1/analises-mg` (paginado)

### Fase 3 — Frontend Angular
- [ ] Camada `core` (models, ClienteService, ViaCepService, interceptor de erro)
- [ ] Tela de listagem (nome, documento, createdAt, updatedAt, status ativo/inativo)
- [ ] Tela de cadastro/edição com Reactive Forms
- [ ] UCs dinâmicas via `FormArray` (adicionar/remover)
- [ ] Validações + feedback visual (snackbar sucesso/erro)
- [ ] Autopreenchimento de endereço por CEP (campos do CEP **read-only**, CEP visível p/ conferência)
- [ ] Listagem: filtro/toggle "incluir inativos" + badges de status
- [ ] Indicação de "cadastro em processamento" (`202`) + reflexo do resultado (pendente/rejeitado) consultando `/cadastros-pendentes/{id}`
- [ ] Badge de status do ViaCEP (lê `/api/integracoes/viacep/status`) + texto explicativo do fluxo assíncrono
- [ ] Tela/lista custom "Análise MG" (lê `/api/v1/analises-mg`) — visualização dos casos do evento (D23)
- [ ] Ação de inativar (soft delete) na listagem

### Fase 4 — Segurança & qualidade
- [ ] Entidades `Usuario`/`Role` + migration + seed de admin (BCrypt)
- [ ] `SecurityConfig` (STATELESS, CSRF off, CORS, regras de rota, `oauth2ResourceServer().jwt()`)
- [ ] `JwtService` (emissão access + refresh) + `JwtAuthenticationConverter` (claims→roles)
- [ ] `AuthController` (`/auth/login`, `/auth/refresh`) + handlers 401/403 em JSON
- [ ] `@PreAuthorize` por role onde fizer sentido
- [ ] Correção de documento restrita a ADMIN (`PATCH /clientes/{id}/documento`) + re-validação de unicidade + auditoria (quem/quando/motivo) — D15
- [ ] Swagger / OpenAPI (springdoc) com esquema `bearerAuth`
- [ ] Testes unitários (service, regras de negócio) — JUnit 5 + MockK
- [ ] Testes de integração (controller/repository) — `@SpringBootTest` / `@WebMvcTest` + Testcontainers (Postgres)
- [ ] Máscaras CEP e documento no Angular
- [ ] Lint: **ktlint/detekt** (Kotlin) + **ESLint** (Angular)
- [ ] **CI GitHub Actions (sem deploy)**: em PR/push p/ `main` → lint + testes (Maven+Testcontainers / ESLint+testes front) + build; **branch protection** na `main` (D31)

### Fase 5 — Imagens de produção & evolução
- [ ] `backend/Dockerfile` multi-stage (build Kotlin → JRE slim)
- [ ] `frontend/Dockerfile` multi-stage (build Angular → Nginx)
- [ ] `docker-compose.prod.yml` (imagens finais, Nginx servindo o Angular)
- [ ] (Opcional futuro) Mensageria real (RabbitMQ/Kafka) para o evento MG, também containerizada

---

## 8. Pontos de atenção / riscos
- **ViaCEP** (D20/D21): backend é fonte da verdade da UF; CEP inexistente → `422`; timeout ~3s; ViaCEP fora do ar → **fila persistente + retry** (`cadastro_pendente`), não degrada.
- **Unicidade da UC**: validar tanto em criação quanto em atualização (não pode "roubar" UC de outro cliente).
- **Soft delete** (D13): usar `@SQLDelete` + filtro **explícito** de "só ativos" (NÃO `@SQLRestriction` global, que esconderia inativos e quebraria a coluna "Status"). Manter service e índice parcial coerentes (`ativo = true`).
- **Regra de UF**: backend enriquece a UF via ViaCEP (não confia no cliente); a UF que gatilha regras vem **sempre** do enriquecimento autoritativo (síncrono ou no retry).
- **Documento**: modelo híbrido (VO + Bean Validation) decidido — atenção ao mapeamento JPA via `AttributeConverter` e à pegadinha de `groups`/`Default` (ver seção 2.4 e Apêndice A).
- **createdAt/updatedAt**: usar auditoria do Spring para não depender do cliente.

### 8.1 Auditoria de riscos e mitigações (revisão crítica das decisões)
| # | Sev | Risco | Mitigação decidida |
|---|-----|-------|---------------------|
| 1 | 🔴 | Fila assíncrona: duplicidade de documento na fila | Defesa em 3 camadas: check no submit (clientes+fila) · índice parcial `WHERE status=PENDENTE` · rede final na criação (ver seção 2.7) |
| 2 | 🔴 | Fila: UX "aceita e depois rejeita" | Validar tudo sem-ViaCEP no submit; front bloqueia UF; só UF-desconhecida adia; badge contextualiza (seção 2.7) |
| 3 | 🔴 | Fila: regra de UF duplicada em 2 caminhos | `finalizarCadastro()` como **fonte única** chamada pelo síncrono e pelo job (seção 2.7) |
| 4 | 🟡 | Volume de features (todas **obrigatórias** — previstas para o bom funcionamento, **não** são scope creep) | Mitigação = **ordem de prioridade** documentada (ver seção 8.2): construir a fundação antes das camadas dependentes. Nada é cortado |
| 5 | 🔴 | Refresh em cookie `httpOnly` cross-origin no dev (4200×8080) | Servir front+back na **mesma origem** via reverse proxy (nginx/proxy do `ng serve`) no dev → cookie same-site |
| 6 | 🟡 | Kotlin + JPA: classes `final`/sem no-arg quebram o Hibernate | Configurar `kotlin-allopen` + `kotlin-noarg` (preset `jpa`) no `pom.xml` (Fase 0) |
| 7 | 🟡 | `data class` em entidade JPA → loop/`LazyInit` em `equals/hashCode/toString` | Entidades = classes normais; **`data class` só em DTOs** |
| 8 | 🟡 | Índice único parcial não é expressável via `@Table` | Criar via **Flyway** (SQL); Hibernate `validate` não checa o predicado (sem conflito) |
| 9 | 🟡 | VO `@JvmInline value class` + JPA pode atritar | Se atritar, usar **classe normal** (não-inline) como VO `Documento` |
| 10 | 🟡 | **Lost update** (2 edições concorrentes do mesmo cliente sobrescrevem-se) | **Optimistic locking** com `@Version` na entidade → `409` em conflito |
| 11 | 🟡 | Exceção no listener `@Async` (evento MG) é **engolida** silenciosamente | `AsyncUncaughtExceptionHandler` + log; idem no job de retry (try/catch → `FALHA`) |
| 12 | 🟡 | Login **sem rate-limit** → brute force | Throttle/lockout por tentativas (ex.: bucket por IP/usuário) — pode ser evolução, mas registrado |
| 13 | 🟡 | Dedup de `numero_instalacao` na **fila** (UCs vivem no JSON do payload) | Extrair instalações para checagem no submit + rede final na criação (índice parcial); cobertura plena na criação |
| 14 | 🔴 | **Segredos** (JWT secret, senha do admin seed) em `.env`/repo | `.env` fora do Git (`.gitignore`) + `.env.example`; secret forte por ambiente; em prod, secret manager |
| 15 | 🟡 | Fuso/`timestamp` inconsistente (createdAt/updatedAt) | Padronizar **UTC** (`Instant`/`timestamptz`); converter no front |

### 8.2 Priorização de features (mitigação de risco — todas obrigatórias)
> **Importante:** **todas** as features fazem parte do escopo e serão entregues — previstas para o bom funcionamento. A prioridade abaixo é apenas a **ordem de execução** (construir a base antes do que depende dela), como mitigação de risco. Não é uma lista de corte.

| Prio | Bloco | Itens | Depende de |
|------|-------|-------|-----------|
| **P0** | Fundação | Docker/Compose, Postgres, Flyway (`V1`), scaffold Kotlin (+plugins JPA), CORS | — |
| **P1** | Domínio + CRUD | Entidades (Cliente/UC/Endereco/VO Documento), repos, DTOs/mappers, CRUD, handler de erro, "últimos 20", soft delete (`@SQLDelete`) | P0 |
| **P2** | Regras de negócio (síncronas) | documento/instalação únicos (índice parcial), `finalizarCadastro()` (UF: bloqueio SP/RS/PR + evento MG), ViaCEP enriquecer síncrono | P1 |
| **P3** | Segurança | JWT (Resource Server), usuários/roles, refresh revogável, storage OWASP, rotas públicas×protegidas | P1 |
| **P4** | Resiliência (feature de destaque) | fila `cadastro_pendente` + retry `@Scheduled` + dedup 3 camadas + `202`; `ViaCepHealthIndicator` + endpoint de status | P2 |
| **P5** | Frontend | listagem (+toggle inativos/badges), cadastro com UCs dinâmicas, autofill (campos read-only), máscaras, indicação de pendente, badge ViaCEP, **UX polida** | P2/P3/P4 (APIs prontas) |
| **P6** | Qualidade & entrega | Swagger, testes unit + integração (Testcontainers), lint (ktlint/detekt + ESLint), **CI GitHub Actions (lint+testes, sem deploy) + branch protection**, Dockerfiles prod, `docker-compose.prod.yml` | demais |

Regra de ouro: **não iniciar um bloco antes de sua dependência estar funcional** (ex.: fila assíncrona P4 depende do `finalizarCadastro` síncrono P2 já existir, pois é a fonte única da regra).

---

## 9. Critérios de aceite (autoavaliação do guia)
- [ ] Código organizado e com separação clara de camadas
- [ ] Responsabilidades bem divididas (controller/service/repository/integration)
- [ ] Implementação clara e legível
- [ ] Boas práticas de arquitetura (DTO, tratamento de erro, validação)
- [ ] Facilidade de manutenção (testes, documentação Swagger)

---

## 10. Planejamento de sprints (ordem de execução)

Sprints alinhados à priorização (8.2) e às dependências. **Testes e lint acompanham cada sprint** (não só no final); o Sprint 6 consolida cobertura + CI. Cada sprint só inicia com a dependência anterior funcional (regra de ouro 8.2).

### Sprint 0 — Fundação & Infra (P0)
- **Objetivo:** ambiente 100% Docker de pé.
- **Entregáveis:** Docker Desktop; monorepo (`/backend`,`/frontend`); scaffold Kotlin+Spring (plugins `allopen`/`noarg`) e Angular standalone; `docker-compose.yml` (db+back+front) com hot reload; **reverse proxy de dev** (mesma origem p/ cookie); `application.yml` (Postgres/Flyway); `V1__init.sql`; CORS; `@Enable*`; `.gitattributes`/`.env.example`.
- **DoD:** `docker compose up` sobe os 3 serviços; backend conecta no Postgres; front serve; Flyway aplica V1.
- **Riscos endereçados:** #5 (cookie/proxy), #6 (Kotlin+JPA), #14 (segredos `.env`).

### Sprint 1 — Domínio & CRUD (P1)
- **Objetivo:** CRUD de clientes funcional.
- **Entregáveis:** entidades (Cliente/UC/`Endereco`/VO `Documento`) com `@Version`; repos; DTOs + mappers (extensão Kotlin); `ClienteService` CRUD; `ClienteController`; soft delete (`@SQLDelete`); handler global (**ProblemDetail/RFC 7807**); paginação (`Pageable`); "últimos 20"; toggle inativos.
- **DoD:** CRUD via Swagger/curl; testes unitários do service; listagem paginada com badge de status.
- **Riscos:** #7 (entidade ≠ data class), #8 (índice parcial via Flyway), #10 (`@Version`), #15 (UTC).

### Sprint 2 — Regras de negócio síncronas (P2)
- **Objetivo:** regras do enunciado no caminho síncrono.
- **Entregáveis:** documento/instalação únicos (índice parcial + service); **`finalizarCadastro()`** (fonte única) com bloqueio **SP/RS/PR → `422`** e **evento MG** → registra em `analise_cliente_mg`; `ViaCepService` (RestClient + timeout) **enriquecendo**; endpoint `GET /analises-mg`.
- **DoD:** regras cobertas por testes unit + **integração (Testcontainers)**; bloqueio e evento verificados.
- **Riscos:** #1/#2/#3 (base da fonte única), #11 (async no listener MG).

### Sprint 3 — Segurança (P3)
- **Objetivo:** auth JWT + autorização.
- **Entregáveis:** `Usuario/Role/RefreshToken` + migration + seed admin (BCrypt); `SecurityConfig` (Resource Server, STATELESS, CSRF off, CORS); `JwtService` (access+refresh) + `JwtAuthenticationConverter`; `AuthController` (login/refresh/logout) com **refresh em cookie httpOnly**; handlers 401/403; matriz **D29** (`@PreAuthorize`); `PATCH /documento` (ADMIN) + auditoria.
- **DoD:** rotas protegidas; login/refresh/logout funcionando via proxy; testes de auth (`@WithMockUser`).
- **Riscos:** #5 (cookie same-origin), #12 (rate-limit — ao menos registrado), #14 (secret JWT).

### Sprint 4 — Resiliência / feature de destaque (P4)
- **Objetivo:** fila de cadastro pendente + retry + health ViaCEP.
- **Entregáveis:** `cadastro_pendente` (+`documento`+índice parcial); `RetryCadastroJob` (`@Scheduled`, backoff/limite/TTL); **`202 Accepted`**; **dedup 3 camadas**; `GET /cadastros-pendentes` (lista) e `/{id}`; `ViaCepHealthIndicator` + `GET /integracoes/viacep/status`.
- **DoD:** simular ViaCEP down → enfileira (202) → retry processa/rejeita; dedup testada; status do ViaCEP exposto.
- **Riscos:** #1 (dedup), #2 (aceita/rejeita), #11 (exceção no job), #13 (instalação na fila).

### Sprint 5 — Frontend (P5)
- **Objetivo:** UI completa e polida consumindo as APIs.
- **Entregáveis:** app standalone; login + interceptor (access em memória) + guards; listagem paginada (+toggle/badges); form cadastro/edição com **UCs dinâmicas (FormArray)** + **máscaras (ngx-mask)** + autofill **read-only**; indicação `202`/pendente; **badge ViaCEP** + explicação; tela de **pendentes**; tela **Análise MG**; UX polida (Material, responsiva, acessível).
- **DoD:** fluxo ponta a ponta no browser; estados loading/erro/pendente claros.
- **Riscos:** UX (D34/feature de destaque visível), #5 (cookie no browser real).

### Sprint 6 — Qualidade & entrega (P6)
- **Objetivo:** consolidar qualidade e empacotar.
- **Entregáveis:** cobertura de testes (unit + integração Testcontainers); lint (ktlint/detekt + ESLint); Swagger com `bearerAuth`; **CI GitHub Actions (lint+testes, sem deploy)** + branch protection na `main`; `Dockerfile` multi-stage (back/front) + `docker-compose.prod.yml`.
- **DoD:** pipeline verde barrando PR ruim; imagens de produção sobem via compose.prod.

> **Observações:** (1) parte do front (Sprint 5) pode começar incrementalmente assim que a API correspondente existir; (2) testes não são exclusivos do Sprint 6 — cada sprint entrega seus testes; (3) os critérios de aceite (seção 9) são verificados ao fim de cada sprint.

---

## Apêndice A — Estratégia de validação (referência de arquitetura)

Guia de referência para decidir **onde cada regra de validação mora** no projeto. Aplica-se a todas as regras de negócio, não só ao documento.

### A.1 — Conceito central: validação ≠ invariante
São a **mesma regra de negócio vista de ângulos diferentes**, tratadas de formas diferentes:

| | Invariante | Validação |
|---|------------|-----------|
| O que é | Define o que o objeto **é**; violar = deixa de ser ele mesmo | Filtro de **entrada externa** antes de chegar ao domínio |
| Onde mora | **No domínio** (Value Object / entidade) | **Na borda** (DTO / application service) |
| Reação à violação | **Exceção** (situação excepcional) | **Erro de resultado** (input ruim é esperado) |

Corolário: regra **universal** do dado → invariante (domínio). Regra **dependente de contexto/endpoint** → validação (borda, via *groups*). Nunca colocar regra contextual dentro do VO.

### A.2 — Camadas de defesa (escolher quantas pelo risco)
```
Frontend  → UX, feedback rápido (não confiável)
Borda/DTO → Bean Validation: rejeita lixo cedo, 400 agregado
Domínio   → Value Object: invariante garantida em qualquer camada
Service   → regras com I/O (ex.: unicidade — precisa do banco)
Banco     → UNIQUE / NOT NULL: última linha
```

### A.3 — Mecânica de *groups* (validação contextual por endpoint)
1. Grupos = interfaces-marcador (`OnCreate`, `OnImportacaoCorporativa`).
2. Constraint declara seus grupos: `@field:ApenasCnpj(groups=[OnImportacaoCorporativa::class])`.
3. Endpoint ativa o grupo: `@Validated(OnImportacaoCorporativa::class)` (Spring) — `@Valid` puro só dispara `Default`.
4. `@GroupSequence` ordena grupos; falha em um grupo interrompe os seguintes.
5. `DefaultGroupSequenceProvider` + `@GroupSequenceProvider` = sequência dinâmica conforme o estado do objeto.
6. **Pegadinha:** ativar grupo não-`Default` desliga o `Default` — incluir explicitamente ou usar sequência.

### A.4 — Regra de bolso (onde cada validação vai)
| Tipo de regra | Onde | Como |
|---------------|------|------|
| Verdade universal do dado | Domínio (VO) | invariante, exceção |
| Filtro de input / formato | Borda (DTO) | Bean Validation |
| Específica de contexto/endpoint | Borda | `@Validated(Grupo)` + groups |
| Depende de estado/I/O (unicidade) | Service | consulta + `409` |
| Última linha | Banco | `UNIQUE` / `NOT NULL` |

### A.5 — Notas Kotlin/JPA
- Use-site target obrigatório: `@field:Documento`.
- VO: `@JvmInline value class` (valor único) ou `data class` `@Embeddable` (múltiplos campos); mapear com `AttributeConverter`.
- *Always-valid domain model*: ao ler do banco, o ORM pode reconstruir o VO sem re-validação — o banco está dentro da fronteira confiável.

### A.6 — Referências
- Hibernate Validator Reference — Grouping constraints: https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/
- Spring Framework — Java Bean Validation (`@Validated`/grupos): https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html
- Baeldung — Grouping Jakarta Validation Constraints: https://www.baeldung.com/javax-validation-groups
- Khorikov — Validation vs Invariants: https://khorikov.org/posts/2022-06-06-validation-vs-invariants/
- Khorikov — Always-Valid Domain Model: https://enterprisecraftsmanship.com/posts/always-valid-domain-model/
- Enterprise Craftsmanship — Validation and DDD: https://enterprisecraftsmanship.com/posts/validation-and-ddd/
- Microsoft Learn — Validations in the domain model layer: https://learn.microsoft.com/en-us/dotnet/architecture/microservices/microservice-ddd-cqrs-patterns/domain-model-layer-validations
- Toptal — Context Validation in DDD: https://www.toptal.com/scala/context-validation-in-domain-driven-design

---

## Apêndice B — (reservado)
*(numeração mantida; conteúdo de riscos/priorização está nas seções 8.1 e 8.2.)*

---

## Apêndice C — Dependências, configuração e esqueletos (referência de implementação)

> Esqueletos para acelerar o arranque. **Pinar as versões estáveis vigentes** no scaffold (política da seção 2) — abaixo, nomes/keys, não versões fixas.

### C.1 — Backend: dependências Maven (Spring Initializr + extras)
- `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`
- `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`
- `spring-boot-starter-actuator`
- `org.postgresql:postgresql`, `org.flywaydb:flyway-core`, `flyway-database-postgresql`
- `org.springframework.boot:spring-boot-devtools` (hot reload)
- `com.fasterxml.jackson.module:jackson-module-kotlin`, `org.jetbrains.kotlin:kotlin-reflect`
- `springdoc-openapi-starter-webmvc-ui` (Swagger)
- **Plugins Kotlin (Maven):** `kotlin-maven-plugin` + `kotlin-maven-allopen` (`all-open` p/ `@Entity`) + `kotlin-maven-noarg` (preset `jpa`)
- **Test:** `spring-boot-starter-test`, `spring-security-test`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`, `io.mockk:mockk`
- **Lint:** ktlint (e/ou detekt) como plugin Maven

### C.2 — `application.yml` (esqueleto, valores por env)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:db}:5432/${DB_NAME:clientes}
    username: ${DB_USER:app}
    password: ${DB_PASSWORD:app}
  jpa:
    hibernate.ddl-auto: validate
    properties.hibernate.jdbc.time_zone: UTC
  flyway.enabled: true
  mvc.problemdetails.enabled: true        # RFC 7807 (D18)
app:
  jwt:
    secret: ${JWT_SECRET}                 # HMAC; NUNCA commitar (risco #14)
    access-ttl: 900                       # 15 min
    refresh-ttl: 604800                   # 7 dias
  viacep:
    base-url: https://viacep.com.br/ws
    timeout-ms: 3000
    health-cache-seconds: 60
  cadastro-pendente:
    max-tentativas: 5
    job-cron: "0 * * * * *"               # a cada 1 min
    ttl-horas: 24
management.endpoints.web.exposure.include: health,info
```

### C.3 — `.env.example` (host/dev) — **`.env` real fora do Git (#14)**
```
DB_NAME=clientes
DB_USER=app
DB_PASSWORD=troque-em-prod
JWT_SECRET=gere-um-segredo-forte-256bits
BACKEND_PORT=8080
FRONTEND_PORT=4200
```

### C.4 — `docker-compose.yml` (dev) — esqueleto
```yaml
services:
  db:
    image: postgres:16          # pinar patch estável
    environment: { POSTGRES_DB: ${DB_NAME}, POSTGRES_USER: ${DB_USER}, POSTGRES_PASSWORD: ${DB_PASSWORD} }
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
  backend:
    image: maven:3-eclipse-temurin-21      # dev: roda ./mvnw spring-boot:run (DevTools)
    working_dir: /app
    command: ./mvnw spring-boot:run
    volumes: ["./backend:/app", "m2:/root/.m2"]
    environment: { DB_HOST: db, JWT_SECRET: ${JWT_SECRET} }
    depends_on: [db]
    ports: ["${BACKEND_PORT}:8080"]
  frontend:
    image: node:20              # dev: ng serve --host 0.0.0.0 --poll
    working_dir: /app
    command: sh -c "npm ci && npx ng serve --host 0.0.0.0 --poll 2000"
    volumes: ["./frontend:/app", "node_modules:/app/node_modules"]
    ports: ["${FRONTEND_PORT}:4200"]
volumes: { pgdata: , m2: , node_modules: }
```
> **Cookie httpOnly do refresh (#5):** em dev, servir front+back na **mesma origem** — usar `proxy.conf.json` do `ng serve` apontando `/api` → `backend:8080` (evita cross-site no cookie). Em prod, Nginx faz o mesmo no `docker-compose.prod.yml`.

### C.5 — Frontend: libs principais
- `@angular/material` + `@angular/cdk`, `ngx-mask`, `@angular/forms` (Reactive Forms).
- Bootstrap standalone (`bootstrapApplication`, `provideRouter`, `provideHttpClient(withInterceptors(...))`).
- `proxy.conf.json` para `/api` (dev).

### C.6 — Migrations Flyway (ordem sugerida)
- `V1__init.sql` — `cliente`, `unidade_consumidora` (+`endereco` embutido), índices **únicos parciais** `WHERE ativo`, `@Version`.
- `V2__cadastro_pendente.sql` — fila + índice `UNIQUE(documento) WHERE status='PENDENTE'`.
- `V3__analise_cliente_mg.sql` — lista do evento MG.
- `V4__seguranca.sql` — `usuario`, `role`, `usuario_role`, `refresh_token` + seed admin.
> Lembrete: migration aplicada é **imutável**; correção = nova migration.
