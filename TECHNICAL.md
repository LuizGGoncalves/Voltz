# Documentação Técnica — Voltz (Gestão de Clientes)

> Documento completo de referência técnica. Para visão geral do produto, ver [`README.md`](README.md).

---

## 1. Modelo de Dados

```mermaid
erDiagram
    CLIENTE ||--o{ UNIDADE_CONSUMIDORA : "1:N"
    CLIENTE {
        bigint id PK
        varchar nome
        varchar documento "UNIQUE WHERE ativo"
        varchar endereco_cep
        varchar endereco_logradouro
        varchar endereco_numero
        varchar endereco_complemento
        varchar endereco_bairro
        varchar endereco_cidade
        varchar endereco_uf
        boolean ativo "soft delete"
        bigint version "optimistic lock"
        timestamptz created_at
        timestamptz updated_at
    }
    UNIDADE_CONSUMIDORA {
        bigint id PK
        varchar nome
        varchar numero_instalacao "UNIQUE WHERE ativo"
        varchar endereco_cep
        varchar endereco_logradouro
        varchar endereco_numero
        varchar endereco_complemento
        varchar endereco_bairro
        varchar endereco_cidade
        varchar endereco_uf
        bigint cliente_id FK
        boolean ativo "soft delete"
        bigint version "optimistic lock"
        timestamptz created_at
        timestamptz updated_at
    }
    CADASTRO_PENDENTE {
        bigint id PK
        varchar documento "UNIQUE WHERE PENDENTE"
        jsonb payload
        varchar status "PENDENTE/PROCESSADO/REJEITADO/FALHA"
        varchar motivo
        int tentativas
        timestamptz created_at
        timestamptz ultima_tentativa
    }
    ANALISE_CLIENTE_MG {
        bigint id PK
        bigint cliente_id FK
        bigint unidade_consumidora_id FK
        varchar status
        timestamptz created_at
    }
    USUARIO ||--o{ USUARIO_ROLE : "N:N"
    ROLE ||--o{ USUARIO_ROLE : "N:N"
    USUARIO {
        bigint id PK
        varchar username "UNIQUE"
        varchar senha "BCrypt"
        boolean ativo
    }
    ROLE {
        bigint id PK
        varchar nome "ADMIN/USER"
    }
    REFRESH_TOKEN {
        bigint id PK
        bigint usuario_id FK
        varchar token_hash "HMAC-SHA256"
        timestamptz expira_em
        boolean revogado
    }
    AUDITORIA_DOCUMENTO {
        bigint id PK
        bigint cliente_id FK
        varchar documento_anterior
        varchar documento_novo
        varchar motivo
        varchar usuario
        timestamptz created_at
    }
```

### Índices parciais (PostgreSQL)
```sql
UNIQUE(documento) WHERE ativo = TRUE          -- cliente
UNIQUE(numero_instalacao) WHERE ativo = TRUE   -- unidade_consumidora
UNIQUE(documento) WHERE status = 'PENDENTE'    -- cadastro_pendente
```

### Migrations
| Versão | Descrição |
|--------|-----------|
| V1 | Schema inicial: todas as tabelas + seed admin |
| V2 | Fix hash BCrypt do admin |
| V3 | Adicionar `version` na unidade_consumidora |
| V4 | Tabela `auditoria_documento` |
| V5 | Adicionar `created_at`/`updated_at` na unidade_consumidora |

---

## 2. API REST

### Autenticação
| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| POST | `/api/v1/auth/login` | Público | Login → access token + cookie refresh |
| POST | `/api/v1/auth/refresh` | Público | Renovar tokens via cookie |
| POST | `/api/v1/auth/logout` | Autenticado | Revogar refresh + limpar cookie |

### Clientes
| Método | Rota | Role | Descrição |
|--------|------|------|-----------|
| POST | `/api/v1/clientes` | USER | Criar (201) ou enfileirar (202) |
| PUT | `/api/v1/clientes/{id}` | USER | Atualizar dados do cliente (sem UCs) |
| GET | `/api/v1/clientes/{id}` | Autenticado | Buscar por ID com UCs |
| GET | `/api/v1/clientes` | Autenticado | Listar paginado (?incluirInativos) |
| GET | `/api/v1/clientes/ultimos` | Autenticado | Últimos 20 ativos |
| PATCH | `/api/v1/clientes/{id}/documento` | ADMIN | Corrigir documento (com auditoria) |
| DELETE | `/api/v1/clientes/{id}` | ADMIN | Soft delete |

### Unidades Consumidoras
| Método | Rota | Role | Descrição |
|--------|------|------|-----------|
| GET | `/api/v1/clientes/{id}/unidades` | USER | Listar UCs do cliente |
| POST | `/api/v1/clientes/{id}/unidades` | USER | Adicionar UC |
| PUT | `/api/v1/clientes/{id}/unidades/{ucId}` | USER | Editar UC |
| DELETE | `/api/v1/clientes/{id}/unidades/{ucId}` | ADMIN | Soft delete UC |

### Pendentes, Análises e Integrações
| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| GET | `/api/v1/cadastros-pendentes` | Autenticado | Listar fila (?status) |
| GET | `/api/v1/cadastros-pendentes/{id}` | Autenticado | Status de um pendente |
| GET | `/api/v1/analises-mg` | Autenticado | Listar eventos MG |
| GET | `/api/v1/integracoes/viacep/status` | Público | Status do ViaCEP |
| GET | `/actuator/health` | Público | Health check |

### Códigos de erro (RFC 7807 ProblemDetail)
| Status | Quando |
|--------|--------|
| 400 | Validação (campos obrigatórios, documento inválido) |
| 401 | Token ausente/inválido/expirado |
| 403 | Sem permissão (role insuficiente) |
| 404 | Cliente ou UC não encontrado |
| 409 | Documento/instalação duplicado, conflito de versão |
| 422 | UF bloqueada (SP/RS/PR), CEP não encontrado |
| 429 | Rate limit excedido (5 tentativas / 15 min) |
| 503 | ViaCEP indisponível |

---

## 3. Segurança

### Fluxo JWT

```mermaid
sequenceDiagram
    participant Browser
    participant API
    participant DB

    Browser->>API: POST /auth/login {username, password}
    API->>API: Rate limit check (bucket4j)
    API->>DB: Buscar usuario (BCrypt verify)
    API->>API: Gerar access token (HMAC-SHA256, 15min)
    API->>API: Gerar refresh token (UUID)
    API->>DB: Salvar hash(refresh) + expiração
    API->>Browser: {accessToken} + Set-Cookie: refresh_token (httpOnly)

    Note over Browser: Access token em memória JS<br/>Refresh token em cookie httpOnly

    Browser->>API: GET /clientes (Authorization: Bearer ...)
    API->>API: JwtDecoder valida assinatura + issuer + expiração
    API->>Browser: 200 dados

    Note over Browser: Access expira (15min)

    Browser->>API: POST /auth/refresh (cookie)
    API->>DB: Validar hash(refresh), não revogado, não expirado
    API->>DB: Revogar refresh antigo
    API->>API: Emitir novo par (access + refresh)
    API->>Browser: {accessToken} + Set-Cookie: novo refresh
```

### Camadas de proteção
| Camada | Mecanismo | O que protege |
|--------|-----------|---------------|
| Rota | SecurityConfig (`authorizeHttpRequests`) | Acesso por URL |
| Método | `@PreAuthorize` | Acesso por role no controller |
| Rate limit | bucket4j (5/15min por IP) | Brute force no login |
| Token | OAuth2 Resource Server + HMAC-SHA256 | Autenticidade |
| Issuer | `JwtIssuerValidator("gestao-clientes-api")` | Segregação entre sistemas |
| Cookie | httpOnly + secure(request.isSecure) + SameSite | XSS + MITM |
| Senha | BCrypt | Hash irreversível |
| Refresh | HMAC-SHA256 hash + revogação + rotação | Roubo de token |
| Concorrência | `@Version` (optimistic locking) | Lost update |

### Profiles
| Profile | Swagger | CORS | Cookie secure |
|---------|---------|------|---------------|
| dev | Público | localhost:4200 | false (HTTP) |
| prod (default) | Protegido | Configurável | true (HTTPS via proxy) |

---

## 4. Backend — Arquitetura

### Pacotes

```mermaid
graph TD
    subgraph web["web (HTTP)"]
        Controller["controller/"]
        DTO["dto/"]
        Mapper["mapper/"]
    end

    subgraph service["service (negócio)"]
        ClienteService
        UCService["UnidadeConsumidoraService"]
        CadPendService["CadastroPendenteService"]
        AnaliseMgService
        RefreshTokenService
        Job["job/RetryCadastroJob"]
        Event["event/AnaliseClienteMgListener"]
    end

    subgraph domain["domain (modelo)"]
        Model["model/ (entidades JPA)"]
        VO["vo/ (Documento)"]
        Rules["rules/ (UfRules)"]
        DomainEvent["event/ (AnaliseClienteMgEvent)"]
    end

    subgraph infra["infraestrutura"]
        Repo["repository/"]
        ViaCep["integration/viacep/"]
        Security["security/"]
        Config["config/"]
    end

    Controller --> service
    service --> domain
    service --> Repo
    service --> ViaCep
    Controller -.-> DTO
    Controller -.-> Mapper
    Security --> Config
```

### Regras de UF (fonte única)

```kotlin
// domain/rules/UfRules.kt — DRY
object UfRules {
    val BLOQUEADAS = setOf("SP", "RS", "PR")
    const val EVENTO_MG = "MG"
}
```

Usada em `ClienteService.finalizarCadastro()` (criação) e `UnidadeConsumidoraService` (UC individual). Mesma regra, um lugar só.

### Fila de retry (cadastro pendente)

```mermaid
flowchart LR
    A[ViaCEP down] --> B[INSERT ON CONFLICT<br/>atômico, sem race condition]
    B --> C[202 Accepted]
    C --> D[Job Scheduled<br/>1 min interval]
    D --> E{ViaCEP up?}
    E -->|Sim| F{UF ok?}
    F -->|Sim| G[PROCESSADO]
    F -->|SP/RS/PR| H[REJEITADO]
    E -->|Não| I{Max tentativas?}
    I -->|< 5| J[Backoff 1,2,4,8,16 min]
    I -->|≥ 5| K[FALHA]
    D --> L{TTL expirado?}
    L -->|> 24h| K
```

### Auditoria de documento

Toda correção via `PATCH /clientes/{id}/documento` grava na tabela `auditoria_documento`:
- `documento_anterior` / `documento_novo`
- `motivo` (obrigatório no request)
- `usuario` (extraído do SecurityContext)
- `created_at`

---

## 5. Frontend — Arquitetura

### Estrutura de componentes

```mermaid
graph TD
    subgraph app["App"]
        AppComponent["AppComponent<br/>(router-outlet)"]
    end

    subgraph layout["Layout"]
        LayoutComponent["LayoutComponent<br/>(sidenav + toolbar + router-outlet)"]
    end

    subgraph features["Features"]
        Login["LoginComponent"]
        Lista["ClienteListaComponent"]
        Form["ClienteFormComponent"]
        Detalhe["ClienteDetalheComponent"]
        UCForm["UcFormComponent (dialog)"]
        Pendentes["PendentesComponent"]
        AnaliseMG["AnaliseMgComponent"]
    end

    subgraph shared["Shared (reutilizáveis)"]
        StatusBadge["StatusBadgeComponent"]
        EmptyState["EmptyStateComponent"]
        EnderecoForm["EnderecoFormComponent"]
        ViaCepBadge["ViaCepBadgeComponent"]
        DocPipe["DocumentoPipe"]
    end

    subgraph core["Core (singleton)"]
        AuthService
        ClienteService
        ViaCepService
        AuthGuard
        AuthInterceptor
    end

    AppComponent --> Login
    AppComponent --> LayoutComponent
    LayoutComponent --> Lista
    LayoutComponent --> Form
    LayoutComponent --> Detalhe
    LayoutComponent --> Pendentes
    LayoutComponent --> AnaliseMG
    Detalhe --> UCForm

    Lista --> StatusBadge
    Lista --> EmptyState
    Lista --> DocPipe
    Detalhe --> StatusBadge
    Detalhe --> EmptyState
    Detalhe --> DocPipe
    Form --> EnderecoForm
    UCForm --> EnderecoForm
    Pendentes --> StatusBadge
    Pendentes --> EmptyState
    AnaliseMG --> StatusBadge
    AnaliseMG --> EmptyState
    LayoutComponent --> ViaCepBadge
```

### Padrões aplicados
| Padrão | Implementação |
|--------|--------------|
| Standalone components | Sem NgModules; cada componente declara seus imports |
| Signals | Estado reativo (loading, erro, dados) |
| Lazy loading | Rotas carregam componentes sob demanda |
| Functional guard | `authGuard` tenta refresh antes de redirecionar |
| Functional interceptor | `authInterceptor` injeta Bearer (exceto ViaCEP externo) |
| Reactive Forms | FormBuilder + FormGroup + Validators |
| Shared components | Avatar, StatusBadge, EmptyState, EnderecoForm, Callout, Skeleton, ConfirmDialog |
| takeUntilDestroyed | Cleanup automático de subscriptions |
| Design System tokens | Todas as cores/sombras/raios via CSS custom properties (nunca hex solto) |
| Tema claro/escuro | `data-theme` no HTML + ThemeService + localStorage |

### Design System — Bolt Energy
| Token | Uso |
|-------|-----|
| `--bolt-500` | Primária (botões, nav ativo, foco) |
| `--accent-500` | Secundária menta (indicadores, realces) |
| `--ok-bg/fg` | Status ativo/processado |
| `--warn-bg/fg` | Status pendente |
| `--danger-bg/fg` | Status inativo/rejeitado |
| `--crit-bg/fg` | Status falha |
| `--surface`, `--bg` | Superfícies e fundo da página |
| `--sidebar` | Menu lateral (sempre escuro) |

Fontes: **Space Grotesk** (títulos) + **Manrope** (corpo). Tema escuro inverte todas as variáveis via `[data-theme="dark"]`.

### Estados de UI (todas as listas)
```
loading → SkeletonComponent (shimmer)
error   → EmptyState com ícone error_outline
empty   → EmptyState com título + subtítulo contextual
data    → tabela Material em card com header + count badge + paginação
```

---

## 6. Infraestrutura

### Docker Compose (dev)
```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  PostgreSQL   │     │  Spring Boot │     │  Angular CLI │
│  :5432        │◄────│  :8080       │     │  :4200       │
│               │     │  (backend)   │     │  (frontend)  │
└──────────────┘     └──────────────┘     └──┬───────────┘
                                              │ proxy.conf.json
                                              │ /api → backend:8080
                                              │ /actuator → backend:8080
                                              │ /swagger-ui → backend:8080
                                              ▼
                                         Browser acessa
                                         localhost:4200
```

### Variáveis de ambiente (.env)
| Variável | Descrição | Obrigatório |
|----------|-----------|-------------|
| `JWT_SECRET` | Chave HMAC (≥ 32 bytes, fail fast) | Sim |
| `POSTGRES_DB/USER/PASSWORD` | Credenciais do banco | Sim |
| `JWT_ACCESS_EXPIRATION_MS` | Expiração do access (default 15min) | Não |
| `JWT_REFRESH_EXPIRATION_MS` | Expiração do refresh (default 7d) | Não |
| `APP_DEMO_SEED` | Carregar dados demo no startup (`true`/`false`) | Não |

### Profiles Spring
| Profile | Ativação | Comportamento |
|---------|----------|---------------|
| `dev` | `SPRING_PROFILES_ACTIVE=dev` no compose | Swagger público, CORS localhost |
| `test` | `@ActiveProfiles("test")` nos testes | JWT secret de teste, rate limit alto |
| (default) | Sem profile = produção | Swagger protegido, CORS vazio, cookie secure |

---

## 7. Testes

### Suítes
| Suite | Tipo | Qtd | Ferramenta |
|-------|------|-----|-----------|
| DocumentoTest | Unitário | 18 | JUnit 5 |
| ClienteServiceTest | Unitário | 7 | JUnit 5 + MockK |
| ClienteApiIntegrationTest | Integração | 8 | Testcontainers PostgreSQL |
| ClientesApplicationTests | Integração | 1 | Testcontainers PostgreSQL |
| **Total** | | **34** | |

### Como rodar
```bash
# Unitários (sem Docker)
docker run --rm -v "$(pwd)/backend:/app" -v maven-cache:/root/.m2 \
  -w /app eclipse-temurin:21-jdk ./mvnw test \
  -Dtest="DocumentoTest,ClienteServiceTest" -B

# Integração (precisa Docker socket)
docker run --rm -v "$(pwd)/backend:/app" -v maven-cache:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  --add-host=host.docker.internal:host-gateway \
  -w /app eclipse-temurin:21-jdk ./mvnw test \
  -Dtest="ClienteApiIntegrationTest,ClientesApplicationTests" -B
```

---

## 8. Auditoria de Segurança (concluída)

28 vulnerabilidades identificadas e tratadas:

| Severidade | Total | Resolvidas | Aceitas |
|-----------|-------|-----------|---------|
| Crítico | 7 | 7 | 0 |
| Alto | 8 | 7 | 1 (infra prod) |
| Médio | 6 | 5 | 1 (migration) |
| Baixo | 7 | 5 | 2 |

Destaques das correções:
- **C1-C3**: Swagger/cookie/JWT controlados por Spring profile + fail fast
- **C4-C5**: Exceções tipadas do Spring Security (401 nativo)
- **C6**: Rate limiting com bucket4j
- **C7**: Defesa em profundidade (SecurityConfig + @PreAuthorize)
- **M1**: INSERT ON CONFLICT DO NOTHING (dedup atômica)
- **M5**: Tabela de auditoria para correção de documento
- **R1-R6**: Refatorações arquiteturais (DRY, service layer, componentização)

Todos os 15 riscos do plano original foram mitigados. Detalhes em commits com prefixo `fix(C*/A*/M*/R*)`.

---

## 9. Correções de boas práticas (pós-auditoria)

Análise adicional contra padrões oficiais Spring Boot + Kotlin identificou e corrigiu:

### Severidade alta
| Item | Antes | Depois | Arquivo |
|------|-------|--------|---------|
| Refresh token hash | SHA-256 puro (rainbow table) | **HMAC-SHA256** com chave JWT | `RefreshTokenService.kt` |
| Retry job transação | Batch-level (inconsistência) | **TransactionTemplate** por item | `RetryCadastroJob.kt` |

### Severidade média
| Item | Correção | Arquivo |
|------|----------|---------|
| Exceção errada | Criada `CadastroPendenteNaoEncontradoException` | `BusinessExceptions.kt` |
| Enriquecimento DRY | `Endereco.enriquecerCom()` (era duplicado em 3 services) | `Endereco.kt` |
| UC sem timestamps | Adicionado `createdAt`/`updatedAt` + migration V5 | `UnidadeConsumidora.kt` |

### Severidade baixa
| Item | Correção |
|------|----------|
| `!!` non-null assertions | Substituído por `requireNotNull()` com mensagens |
| `ex.message!!` | Substituído por `ex.message.orEmpty()` |
| `Optional<T>` nos repos | Padronizado para `T?` (Kotlin idiomático) |
| `@EnableJpaAuditing` | Já existia — confirmado |

---

## 10. Seeder de demonstração

Classe `DemoSeeder` (`config/DemoSeeder.kt`) — `ApplicationRunner` condicional.

**Ativação:** `APP_DEMO_SEED=true` (variável de ambiente ou property).
**Idempotente:** Só roda se `clienteRepository.count() == 0`.

### Dados criados

| Entidade | Qtd | Detalhe |
|----------|-----|---------|
| Clientes | 10 | 9 ativos + 1 inativo (SP). CPFs e CNPJs reais (válidos em estrutura). Cidades: BH, RJ, SP, Salvador, Brasília, Fortaleza |
| UCs | 13 | Distribuídas entre clientes. 5 em MG (geram análises), restante em RJ, BA, DF, CE |
| Pendentes | 5 | 1 PENDENTE, 2 REJEITADO (PR e SP), 1 PROCESSADO, 1 FALHA (TTL) |
| Análises MG | 5 | Todas PENDENTE_ANALISE, vinculadas às UCs em MG |

### O que cada tela mostra com o seeder

| Tela | O que aparece |
|------|---------------|
| Lista de Clientes | 9 clientes ativos com avatares, busca funcional, toggle mostra o inativo |
| Detalhe | Clientes com 1-3 UCs em cards, endereços completos |
| Novo Cliente | Formulário vazio pronto para criar |
| Pendentes | 5 registros nos 4 status possíveis (todos os filtros funcionam) |
| Análises MG | 5 registros com links para os clientes |
