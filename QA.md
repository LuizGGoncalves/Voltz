# QA — Plano de Testes e Checklist

> **O que é este arquivo:** checklist reutilizável de QA para validar o sistema antes de cada entrega.
> Cobre todas as features, regras de negócio, segurança e edge cases.
> **Última execução:** 2026-06-04 — **50 testes, 0 bugs pendentes**.

---

## Como executar

```bash
# 1. Subir o ambiente limpo
docker compose down -v && docker compose up -d --build

# 2. Aguardar backend
# Verificar: curl http://localhost:4200/actuator/health → {"status":"UP"}

# 3. Obter token
TOKEN=$(curl -s -X POST http://localhost:4200/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
```

---

## 1. Autenticação & Segurança

| # | Teste | Método | Esperado | Status |
|---|-------|--------|----------|--------|
| T01 | Login com credenciais válidas | POST /auth/login | 200 + accessToken | ✅ |
| T02 | Login com senha errada | POST /auth/login | 401 | ✅ |
| T03 | Login com username inexistente | POST /auth/login | 401 | ✅ |
| T04 | Login com body vazio `{}` | POST /auth/login | 400 | ✅ |
| T05 | Login sem body | POST /auth/login | 400 | ✅ |
| T06 | Rota protegida sem token | GET /clientes | 401 | ✅ |
| T07 | Rota protegida com token inválido | GET /clientes | 401 | ✅ |
| T08 | Refresh token via cookie | POST /auth/refresh | 200 + novo token | ✅ |
| T09 | Refresh sem cookie | POST /auth/refresh | 401 | ✅ |
| T10 | Logout | POST /auth/logout | 204 + cookie limpo | ✅ |
| T11 | Health pública sem token | GET /actuator/health | 200 | ✅ |
| T12 | ViaCEP status público sem token | GET /integracoes/viacep/status | 200 | ✅ |
| T13 | Swagger UI sem token | GET /swagger-ui/index.html | 200 | ✅ |

---

## 2. Validações de Entrada (CRUD)

| # | Teste | Esperado | Status |
|---|-------|----------|--------|
| T14 | Criar com body vazio `{}` | 400 com errors[] | ✅ |
| T15 | Criar sem nome (vazio) | 400 "Nome é obrigatório" | ✅ |
| T16 | CPF com dígitos verificadores errados | 400 "Documento inválido" | ✅ |
| T17 | CPF com todos os dígitos iguais (00000000000) | 400 | ✅ |
| T18 | CNPJ inválido | 400 | ✅ |
| T19 | Documento com letras | 400 | ✅ |
| T20 | CEP inexistente (00000000) | 422 "CEP não encontrado" | ✅ |
| T21 | Sem unidades consumidoras (lista vazia) | 400 "Pelo menos uma UC" | ✅ |
| T22 | SQL injection no nome (`'; DROP TABLE`) | 201 — armazena como texto | ✅ |
| T23 | XSS no nome (`<script>alert(1)</script>`) | 201 — armazena como texto | ✅ |
| T24 | Nome com 300 caracteres (max 255) | 400 | ✅ |
| T25 | Content-Type errado (text/plain) | 415 | ✅ |

### Segurança de dados
- **SQL injection**: o JPA/Hibernate usa prepared statements — a string é armazenada literalmente, sem execução.
- **XSS**: o backend armazena como texto. O Angular sanitiza na renderização por padrão.
- **Constraint enforcement**: o handler global captura `DataIntegrityViolationException` → 409.

---

## 3. Regras de Negócio

| # | Teste | Esperado | Status |
|---|-------|----------|--------|
| T26 | Criar cliente com UC em MG (enriquecimento ViaCEP) | 201 + endereço enriquecido | ✅ |
| T27 | Evento MG registrado após criar UC em MG | analise_cliente_mg com registro | ✅ |
| T28 | Documento duplicado (mesmo CPF ativo) | 409 | ✅ |
| T29 | Instalação duplicada (mesma UC ativa) | 409 | ✅ |
| T30 | UC em SP → bloqueio | 422 "SP não é permitida" | ✅ |
| T31 | UC em RS → bloqueio | 422 | ✅ |
| T32 | UC em PR → bloqueio | 422 | ✅ |
| T33 | UC em RJ → permitida | 201 | ✅ |
| T34 | Soft delete (DELETE) | 204 + ativo=false no banco | ✅ |
| T35 | Listar só ativos (default) | Exclui inativos | ✅ |
| T36 | Listar com inativos (toggle) | Inclui todos | ✅ |
| T37 | Recadastrar documento de inativado | 201 — índice parcial WHERE ativo | ✅ |
| T38 | Últimos 20 | 200 com registros ordenados | ✅ |
| T39 | GET por ID com UCs (fetch join) | 200 com unidadesConsumidoras | ✅ |
| T40 | GET ID inexistente | 404 | ✅ |
| T41 | PATCH documento (ADMIN) | 200 + documento atualizado | ✅ |
| T42 | Cadastros pendentes (lista vazia) | 200 totalElements=0 | ✅ |

---

## 4. Edge Cases & Operações Complexas

| # | Teste | Esperado | Status |
|---|-------|----------|--------|
| T43 | PUT atualizar cliente (mantendo mesma UC) | 200 + dados atualizados | ✅ |
| T44 | PUT com UC em SP (bloqueio no atualizar) | 422 | ✅ |
| T45 | Criar com 3 UCs simultâneas | 201 + 3 UCs | ✅ |
| T46 | Paginação (page=0, size=1) | 1 item, totalPages correto | ✅ |
| T47 | CNPJ válido com máscara | 201 tipo=CNPJ | ✅ |
| T49 | Análises MG com paginação | 200 + registros MG | ✅ |
| T50 | PUT trocando UC (remove velha, adiciona nova) | 200 | ✅ |

---

## 5. Bugs Encontrados e Corrigidos

| Bug | Descrição | Causa Raiz | Fix |
|-----|-----------|-----------|-----|
| BUG-01 | PUT /clientes retorna 500 ao atualizar UCs | `clear()+addAll` com `orphanRemoval` + índice único parcial: Hibernate executa INSERT antes do DELETE soft | Refatorado para atualizar UCs existentes in-place por `numeroInstalacao`, adicionar novas, e remover obsoletas |
| BUG-02 | `DataIntegrityViolationException` retorna 500 genérico | Sem handler para violação de constraint do banco | Adicionado handler → 409 com mensagem clara |

---

## 6. Boas Práticas de QA Adotadas

### Pirâmide de testes
```
        /  E2E (curl/browser)  \     ← este checklist
       / Integração (Testcontainers) \
      /   Unitários (MockK/JUnit)     \
```

### Categorias de teste
1. **Happy path**: fluxo principal funciona (criar, listar, editar, deletar)
2. **Validação de entrada**: campos obrigatórios, formatos, limites de tamanho
3. **Regras de negócio**: unicidade, bloqueios, eventos, soft delete
4. **Segurança**: autenticação, autorização, injeção (SQL/XSS)
5. **Edge cases**: concorrência, dados limítrofes, operações compostas
6. **Resiliência**: ViaCEP indisponível, retry, fila

### Princípios seguidos
- **Testar o contrato, não a implementação**: verificar HTTP status + body, não o código interno
- **Cada teste é independente**: pode rodar em qualquer ordem
- **Testar os erros primeiro**: validações negativas são mais reveladoras que happy path
- **Boundary testing**: testar nos limites (campo vazio, max length, lista vazia)
- **Fail fast, fail loud**: erros devem retornar status e mensagem claros, nunca 500 genérico

### Quando rodar
- Antes de cada entrega/merge
- Após refatorações significativas
- Ao adicionar nova feature (adicionar testes correspondentes)

---

## 7. Como adicionar novos testes

1. Adicione a linha na seção correspondente (auth, validação, regra, edge case)
2. Implemente o curl de teste
3. Execute e registre o resultado
4. Se falhar, registre na seção de Bugs com causa raiz e fix

> **Convenção**: manter este arquivo atualizado a cada ciclo de QA. Numerar testes sequencialmente (T51, T52...).
