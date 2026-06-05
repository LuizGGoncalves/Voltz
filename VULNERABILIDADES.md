# Auditoria de Vulnerabilidades e Gaps de Implementação

> **Gerado em:** 2026-06-04 — análise estática completa do código vs PLANO.md
> **Escopo:** 16 arquivos backend + infra analisados, cruzados com seções 2.5, 2.9, 8.1 do PLANO.md
> **Status:** pendente de correção

---

## Resumo Executivo

| Severidade | Qtd | Ação |
|-----------|-----|------|
| CRITICO | 7 | Fix imediato antes de qualquer exposição |
| ALTO | 8 | Corrigir antes de considerar "feature complete" |
| MEDIO | 6 | Resolver antes de produção |
| BAIXO | 7 | Melhorias recomendadas |

---

## CRITICO — Fix Imediato

### ~~C1. Swagger público (viola D29 do PLANO)~~ — RESOLVIDO
- **Onde:** `SecurityConfig.kt:45`
- **Problema:** `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()` — PLANO diz Swagger protegido (autenticado)
- **Risco:** Expõe toda a estrutura da API publicamente (OWASP A05:2021)
- **Solução aplicada:** Swagger controlado por `springdoc.swagger-ui.public-access` (default: `false` = protegido). Em dev, `SPRINGDOC_PUBLIC_ACCESS=true` via env var. **Justificativa:** velocidade no desenvolvimento local (sem precisar copiar token para cada teste no Swagger) com segurança em produção (default protegido — esquecer a config não expõe nada)

### ~~C2. Cookie `secure=false` sem parametrização por profile~~ — RESOLVIDO
- **Onde:** `AuthController.kt`
- **Problema:** `secure(false)` hardcoded — se deploy acidental em prod, refresh token viaja em HTTP puro
- **Solução aplicada:** `secure(request.isSecure)` — o cookie acompanha o protocolo da requisição automaticamente. Em dev (HTTP) fica `false`, em prod (HTTPS via proxy) fica `true` sem config manual. Configurado `server.forward-headers-strategy: framework` para que o Spring leia `X-Forwarded-Proto` de proxies. **Justificativa:** padrão Spring Boot para apps atrás de proxy reverso; zero config manual = zero risco de esquecer

### ~~C3. JWT Secret padrão no código + padding inseguro~~ — RESOLVIDO
- **Onde:** `application.yml`, `JwtService.kt`, `SecurityConfig.kt`
- **Problema:** Secret default visível no código; se < 32 bytes, faz padding com zeros; lógica duplicada em 2 locais
- **Solução aplicada (4 pontos):**
  1. **Bean centralizado** (`JwtKeyConfig`): `SecretKeySpec` criada uma vez, injetada em `JwtService` (assinar) e `jwtDecoder` (validar). Responsabilidades preservadas, chave unificada. Facilita migração futura para Authorization Server externo
  2. **Fail fast**: `require(keyBytes.size >= 32)` — app não sobe se secret fraco. Sem padding silencioso
  3. **Profiles Spring**: `application-dev.yml` com overrides de dev (swagger público). Em prod, default seguro. Controlado por `SPRING_PROFILES_ACTIVE=dev` no compose
  4. **Removido boolean solto**: `SPRINGDOC_PUBLIC_ACCESS` env var eliminada; swagger controlado pelo profile
- **JWT_SECRET** obrigatório via `.env` (sem default no yml); `.env.example` tem valor dev-ready

### ~~C4. `.get()` unsafe no AuthController~~ — RESOLVIDO
- **Onde:** `AuthController.kt`
- **Problema:** `.get()` lança `NoSuchElementException` → 500 genérico
- **Solução aplicada:** `.orElseThrow { UsernameNotFoundException(...) }` — exceção do Spring Security, tratada nativamente como 401 pelo framework. Zero código extra de handler

### ~~C5. RuntimeException genérica no RefreshTokenService~~ — RESOLVIDO
- **Onde:** `RefreshTokenService.kt`
- **Problema:** `RuntimeException` sem handler → 500 genérico
- **Solução aplicada:** `UsernameNotFoundException` (Spring Security) — tratado nativamente como 401. Mesmo padrão do C4

### ~~C6. Sem rate limiting no login (Risco #12 do PLANO)~~ — RESOLVIDO
- **Onde:** `AuthController.kt`
- **Problema:** Brute force ilimitado no `/auth/login`
- **Solução aplicada:** `bucket4j` com `RateLimitService` — 5 tentativas a cada 15 minutos por IP. Excedido → 429 Too Many Requests (ProblemDetail). Log de warning com IP. `RateLimitExceededException` + handler no GlobalExceptionHandler

### ~~C7. Logout/Refresh sem `@PreAuthorize` explícito~~ — RESOLVIDO
- **Onde:** `AuthController.kt` + `SecurityConfig.kt`
- **Problema:** `/api/v1/auth/**` como `permitAll()` genérico; sem defesa em profundidade
- **Solução aplicada:**
  - SecurityConfig: rotas de auth granulares (`/login` permitAll, `/refresh` permitAll, `/logout` authenticated) em vez do wildcard `/**`
  - `/logout`: `@PreAuthorize("isAuthenticated()")` — defesa em profundidade (2 camadas: SecurityConfig + método)
  - `/refresh`: `permitAll()` porque o access token pode estar expirado (cliente só tem o cookie); proteção vem da validação do refresh token no `RefreshTokenService`
  - **Justificativa:** separar rotas explicitamente evita que alterações futuras no SecurityConfig exponham endpoints por acidente. O `@PreAuthorize` no logout garante proteção mesmo se a regra de rota mudar

---

## ALTO — Corrigir Antes de Feature Complete

### ~~A1. Falta `@PreAuthorize` no ClienteController (D29)~~ — RESOLVIDO
- **Solução:** POST/PUT `hasRole('USER')`, PATCH/DELETE `hasRole('ADMIN')`. Defesa em profundidade

### ~~A2. Sem AsyncUncaughtExceptionHandler (Risco #11)~~ — RESOLVIDO
- **Solução:** try/catch no listener MG + `AsyncConfig` global como rede de segurança

### ~~A3. Falta validação de JWT issuer~~ — RESOLVIDO
- **Solução:** `issuer("gestao-clientes-api")` na emissão + `JwtIssuerValidator` no decoder

### ~~A4. Sem logging de auth~~ — RESOLVIDO
- **Solução:** log.info em login sucesso/refresh/logout, log.warn em falha/rate limit (OWASP A09)

### ~~A5. CORS hardcoded para localhost~~ — RESOLVIDO
- **Solução:** `cors.allowed-origins` em `application.yml` (vazio por padrão = nenhum), valor de dev em `application-dev.yml`. Controlado por profile

### ~~A6. Sem handler OptimisticLockingFailureException~~ — RESOLVIDO
- **Solução:** Handler → 409 "Registro modificado por outro usuário" (risco #10 PLANO)

### ~~A7. Falta `@Version` na UnidadeConsumidora~~ — RESOLVIDO
- **Contexto:** UC ganhou endpoints independentes (CRUD separado do Cliente), então precisa de proteção própria contra edições concorrentes
- **Solução aplicada:** `@Version` na UC + migration V3 + `@SQLDelete` com version na query. Endpoints: POST/PUT/DELETE /clientes/{id}/unidades/{ucId} com @PreAuthorize

### A8. Dockerfiles/compose de produção — PENDENTE (aguarda infra)
- **PLANO:** Dockerfiles multi-stage + docker-compose.prod.yml
- **Status:** aguardando definição de ambiente de deploy (AWS). Quando definido: Dockerfiles multi-stage (build → JRE slim / build → Nginx), nginx.prod.conf com HTTPS/HSTS, docker-compose.prod.yml com secrets injetados via AWS Secrets Manager

---

## MEDIO — Antes de Produção

### ~~M1. Race condition na dedup da fila~~ — RESOLVIDO
- **Solução:** `INSERT ON CONFLICT DO NOTHING` (Postgres nativo, atômico). Sem gap entre check e insert. Risco #1 PLANO mitigado

### ~~M2. Senha admin em migration~~ — ACEITO
- **Status:** migrations V1/V2 já aplicadas, imutáveis (Flyway checksum). Hash na V1 não é o correto (corrigido na V2). Em prod, seed via script externo + AWS Secrets Manager

### ~~M3. Sem contexto de usuário nos logs~~ — RESOLVIDO
- **Solução:** `SecurityUtils.currentUsername()` em todos os logs de negócio (criar, atualizar, inativar, corrigir documento). OWASP A09

### ~~M4. Retry job sem paginação~~ — RESOLVIDO
- **Solução:** `LIMIT 50` no `findPendentesParaRetry()`. Batch size configurável

### ~~M5. PATCH documento sem auditoria~~ — RESOLVIDO
- **Solução:** Tabela `auditoria_documento` (V4 migration) com cliente_id, documento_anterior, documento_novo, motivo, usuario, created_at. Gravada automaticamente no `corrigirDocumento()`. Consultável por clienteId

### ~~M6. Dedup instalação na fila~~ — RESOLVIDO
- **Solução:** `CadastroPendenteService.enfileirar()` agora valida `numeroInstalacao` do request contra UCs ativas. Risco #13 PLANO mitigado

---

## BAIXO — Melhorias Recomendadas

### ~~B1. Sem validação min/max nos tempos de expiração do JWT~~ — RESOLVIDO
- **Solução:** `@Min`/`@Max` + `@Validated` no JwtProperties. Access: 1min–1h. Refresh: 1h–30d

### ~~B2. Health endpoint expõe componentes~~ — ACEITO
- **Decisão:** manter público. Padrão para monitoramento (uptime checks, load balancer). Detalhes de componentes só com auth (`show-components: when-authorized`)

### ~~B3. Payload JSON no CadastroPendenteResponse~~ — JÁ OK
- **Status:** DTO já não expõe o campo payload. Sem mudança necessária

### ~~B4. Comentário no Documento VO~~ — RESOLVIDO
- **Solução:** comentário explicando por que não usar `@JvmInline value class` (risco #9 PLANO)

### ~~B5. setInterval sem cleanup~~ — RESOLVIDO
- **Solução:** `ngOnDestroy` com `clearInterval`. Sem memory leak

### ~~B6. AuthGuard sem refresh~~ — RESOLVIDO
- **Solução:** guard tenta `authService.refresh()` antes de redirecionar ao login. Se o cookie httpOnly ainda for válido, renova o token silenciosamente. Melhor UX: page reload não força re-login

### ~~B7. Falta docker-compose.prod.yml~~ — REMOVIDO
- **Contexto:** nginx removido do projeto (substituído por proxy Angular CLI). Produção será definida com infra AWS. Igual ao A8

---

## Cruzamento: Riscos do PLANO vs Status

| # | Risco (PLANO 8.1) | Mitigação planejada | Implementado? | Gap |
|---|-------------------|---------------------|---------------|-----|
| 1 | Dedup na fila | 3 camadas | Parcial | Race condition (M1) + falta dedup instalação (M6) |
| 2 | "Aceita e depois rejeita" | Validar sem-ViaCEP no submit | Sim | Front não bloqueia UF (sprint 5 confia no back) |
| 3 | Regra duplicada em 2 caminhos | `finalizarCadastro()` | Sim | OK |
| 5 | Cookie cross-origin | Nginx proxy | Sim | Cookie secure=false hardcoded (C2) |
| 6 | Kotlin + JPA classes final | allopen/noarg | Sim | OK |
| 7 | data class em entidade | Classes normais | Sim | OK |
| 8 | Índice parcial não expressável | Flyway | Sim | OK |
| 9 | Value class + JPA | Classe normal | Sim | Falta comentário (B4) |
| 10 | Lost update | @Version | Parcial | Sem handler 409 (A6) + UC sem version (A7) |
| 11 | @Async engolido | AsyncHandler + log | Não | Falta handler (A2) |
| 12 | Login sem rate-limit | Throttle | Não | Falta implementar (C6) |
| 13 | Dedup instalação na fila | Extrair + checar | Não | Falta implementar (M6) |
| 14 | Segredos em .env | .gitignore + example | Sim | Secret default no yml (C3) + senha no SQL (M2) |
| 15 | Fuso timestamp | UTC Instant | Sim | OK |

---

> **Próximo passo:** priorizar e resolver os CRITICOs, depois os ALTOs. Cada fix deve ser testado e registrado no QA.md.
