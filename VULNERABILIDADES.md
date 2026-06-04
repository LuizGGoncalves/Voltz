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

### C1. Swagger público (viola D29 do PLANO)
- **Onde:** `SecurityConfig.kt:45`
- **Problema:** `.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()` — PLANO diz Swagger protegido (autenticado)
- **Risco:** Expõe toda a estrutura da API publicamente (OWASP A05:2021)
- **Fix:** Remover `permitAll()` do Swagger; em dev, usar profile condicional

### C2. Cookie `secure=false` sem parametrização por profile
- **Onde:** `AuthController.kt:105`
- **Problema:** `secure(false)` hardcoded — se deploy acidental em prod, refresh token viaja em HTTP puro
- **Fix:** Parametrizar via `application.yml` com valor por profile (dev=false, prod=true)

### C3. JWT Secret padrão no código + padding inseguro
- **Onde:** `application.yml:34`, `JwtService.kt:18`, `SecurityConfig.kt:73`
- **Problema:** Secret default visível no código; se < 32 bytes, faz padding com zeros (enfraquece chave); lógica duplicada em 2 locais
- **Fix:** Remover default; validar >= 32 bytes no `@PostConstruct`; centralizar criação da chave

### C4. `.get()` unsafe no AuthController
- **Onde:** `AuthController.kt:40,64`
- **Problema:** `usuarioRepository.findByUsername(...).get()` lança `NoSuchElementException` → 500 genérico se usuário deletado entre login e refresh
- **Fix:** Usar `.orElseThrow { ... }` com exceção semântica

### C5. RuntimeException genérica no RefreshTokenService
- **Onde:** `RefreshTokenService.kt:31,34`
- **Problema:** `RuntimeException("Refresh token inválido")` sem handler → 500 genérico em vez de 401
- **Fix:** Criar exceções tipadas + handler no GlobalExceptionHandler → 401

### C6. Sem rate limiting no login (Risco #12 do PLANO)
- **Onde:** `AuthController.kt` — nenhuma proteção
- **Problema:** Brute force ilimitado no `/auth/login`
- **Fix:** Implementar throttle por IP/username (bucket4j ou similar); mínimo: log de falhas

### C7. Logout/Refresh sem `@PreAuthorize` explícito
- **Onde:** `AuthController.kt`
- **Problema:** Depende apenas do SecurityConfig; sem defesa em profundidade
- **Fix:** Adicionar `@PreAuthorize("isAuthenticated()")` em `/logout` e `/refresh`

---

## ALTO — Corrigir Antes de Feature Complete

### A1. Falta `@PreAuthorize` nos métodos do ClienteController (D29)
- **Onde:** `ClienteController.kt`
- **PLANO:** POST/PUT = USER, DELETE/PATCH = ADMIN
- **Atual:** Sem @PreAuthorize; só SecurityConfig protege por rota
- **Fix:** Adicionar `@PreAuthorize("hasRole('USER')")` no POST/PUT, `hasRole('ADMIN')` no DELETE/PATCH

### A2. Sem AsyncUncaughtExceptionHandler (Risco #11 do PLANO)
- **Onde:** `AnaliseClienteMgListener.kt` — sem try/catch; exceções engolidas
- **Fix:** Configurar `AsyncUncaughtExceptionHandler` bean + try/catch no listener

### A3. Falta validação de JWT issuer/audience
- **Onde:** `JwtService.kt` — token sem `iss`/`aud`
- **Problema:** Token de outro sistema com mesma chave seria aceito
- **Fix:** Adicionar `.issuer("gestao-clientes")` na emissão e validação

### A4. Sem logging de eventos de segurança
- **Onde:** `AuthController.kt`, `RefreshTokenService.kt`
- **Problema:** Login (sucesso/falha), logout, refresh — nenhum registrado
- **Fix:** `log.info/warn` em todos os fluxos de auth

### A5. CORS hardcoded para localhost
- **Onde:** `SecurityConfig.kt:88`
- **Fix:** Parametrizar via env var `CORS_ALLOWED_ORIGINS`

### A6. Sem handler para OptimisticLockingFailureException (Risco #10 do PLANO)
- **Onde:** `GlobalExceptionHandler.kt`
- **Problema:** `@Version` no Cliente existe mas exception vira 500
- **Fix:** Handler → 409 "Cliente modificado por outro usuário"

### A7. Falta `@Version` na UnidadeConsumidora
- **Onde:** `UnidadeConsumidora.kt`
- **Problema:** `@SQLDelete` sem version; concurrent deletes sem conflito
- **Fix:** Adicionar `@Version` e ajustar SQLDelete query

### A8. Dockerfiles/compose de produção não entregues (Sprint 6)
- **PLANO:** Dockerfiles multi-stage + docker-compose.prod.yml
- **Atual:** Só dev
- **Fix:** Criar stages de prod com HTTPS, secure cookies, secrets injetados

---

## MEDIO — Antes de Produção

### M1. Race condition na dedup da fila (Risco #1 do PLANO)
- **Onde:** `CadastroPendenteService.enfileirar()`
- **Problema:** Check-then-insert sem isolation → gap entre verificação e INSERT
- **Fix:** `SERIALIZABLE` ou `INSERT ON CONFLICT DO NOTHING`

### M2. Senha do admin seed exposta em migration + comentário
- **Onde:** `V1__init.sql:109-110`, `V2__fix_admin_password.sql`
- **Fix:** Remover comentário com senha; usar script separado para seed

### M3. Sem contexto de usuário nos logs de negócio
- **Onde:** Services e controllers
- **Problema:** Não registra *quem* criou/editou/inativou (OWASP A09)
- **Fix:** Injetar `SecurityContextHolder.getContext().authentication.name` nos logs

### M4. Paginação ausente no RetryCadastroJob
- **Onde:** `RetryCadastroJob.kt:37`
- **Problema:** `findPendentesParaRetry()` retorna TODOS; se milhares, OOM
- **Fix:** Adicionar LIMIT/batch size

### M5. PATCH documento sem auditoria (D15 do PLANO)
- **Onde:** `ClienteService.corrigirDocumento()`
- **PLANO:** "re-validação de unicidade + **auditoria** (quem/quando/motivo)"
- **Atual:** Re-valida, mas não registra quem/quando/motivo
- **Fix:** Gravar log de auditoria com username, documento antigo/novo, motivo

### M6. Dedup de `numero_instalacao` na fila incompleta (Risco #13 do PLANO)
- **Onde:** `CadastroPendenteService.enfileirar()`
- **PLANO:** "Extrair instalações para checagem no submit"
- **Atual:** Só checa documento, não instalações dentro do payload JSON
- **Fix:** Extrair e validar `numeroInstalacao` do request contra UCs ativas

---

## BAIXO — Melhorias Recomendadas

### B1. Sem validação min/max nos tempos de expiração do JWT
### B2. Health endpoint expõe componentes sem autenticação
### B3. `CadastroPendenteResponse` pode expor payload JSON bruto (data leak)
### B4. PLANO sugere tentar `@JvmInline value class` no Documento (risco #9) — atual é correto, falta comentário
### B5. Frontend: `setInterval(30000)` para ViaCEP status sem cleanup (memory leak no Angular)
### B6. Frontend: AuthGuard não tenta refresh antes de redirecionar ao login
### B7. Falta `docker-compose.prod.yml` e nginx prod com HTTPS/HSTS

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
