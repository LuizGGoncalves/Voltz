# Gestão de Clientes — Visão do Projeto (documento vivo)

> **O que é este arquivo:** o **espelho** da aplicação — explica, em linguagem acessível, **o que** o sistema faz e **por que** cada escolha foi feita. É um **documento vivo**: as decisões podem mudar conforme o desenvolvimento, e essas mudanças ficam registradas no **histórico (seção 7)**.
>
> **Para construir o sistema** (spec técnica detalhada, contratos, esqueletos de config): ver **[`PLANO.md`](PLANO.md)**.
> **Base do exercício:** *Guia Prático de Treinamento Fullstack (Java/Kotlin + Angular)*.
> **Última atualização:** 2026-06-04 · **Status do projeto:** planejamento concluído, implementação não iniciada.

---

## Sumário
1. [Sobre o projeto](#1-sobre-o-projeto)
2. [O que a aplicação faz](#2-o-que-a-aplicação-faz)
3. [Stack e o porquê](#3-stack-e-o-porquê)
4. [Como rodar (dev 100% Docker)](#4-como-rodar-dev-100-docker)
5. [Arquitetura num olhar](#5-arquitetura-num-olhar)
6. [Decisões de arquitetura (ADR)](#6-decisões-de-arquitetura-adr)
7. [Histórico de mudanças de decisão](#7-histórico-de-mudanças-de-decisão)
8. [Glossário (para qualquer leitor)](#8-glossário-para-qualquer-leitor)
9. [Riscos conhecidos](#9-riscos-conhecidos)

---

## 1. Sobre o projeto

Aplicação **fullstack** para **gestão de clientes** e suas **unidades consumidoras (UC)**. Um cliente (pessoa física ou jurídica) pode ter **várias** unidades consumidoras (ex.: imóveis com instalações). O sistema cobre todo o ciclo: cadastrar, atualizar, listar, consultar e inativar — com regras de negócio específicas (validação de documento, endereço por CEP, bloqueios por estado, eventos).

O foco do exercício é **qualidade de engenharia**: organização, separação de responsabilidades, clareza e boas práticas — por isso muitas decisões aqui priorizam *fazer certo* mesmo quando *fazer simples* bastaria.

## 2. O que a aplicação faz

**Funcionalidades**
- Cadastrar, atualizar, listar (paginado), consultar por ID e **inativar** clientes (exclusão lógica — o dado nunca some do banco).
- Exibir os **últimos 20** clientes.
- Cada cliente com **N unidades consumidoras**, adicionadas/removidas dinamicamente no formulário.

**Regras de negócio**
- **Não permite documento duplicado** (CPF/CNPJ válidos, com dígitos verificadores).
- Uma **unidade consumidora não pode pertencer a dois clientes**.
- **Endereço preenchido pelo CEP** (via API ViaCEP).
- **Bloqueia** cadastro de unidade em **SP, RS ou PR**.
- Unidade em **MG** dispara um **evento** (`analise_cliente_mg`) que entra numa lista para análise futura.

**Diferenciais (feature de destaque)**
- Se a consulta de CEP estiver fora do ar, o cadastro **não se perde**: entra numa **fila** e é processado automaticamente quando o serviço volta — com **acompanhamento na tela**.
- **Indicador** na interface mostrando se a consulta de CEP está disponível.

## 3. Stack e o porquê

| Camada | Tecnologia | Por que (resumo) |
|--------|-----------|------------------|
| Backend | **Kotlin + Spring Boot** | Conciso e seguro; ecossistema maduro |
| Build | **Maven** | Exigido pelo enunciado |
| Dados | **PostgreSQL + JPA/Hibernate** | Banco real, robusto; ORM padrão |
| Schema | **Flyway** | Histórico versionado do banco (quem mudou o quê, quando) |
| Segurança | **Spring Security + JWT** | Padrão para SPA + API; login com papéis (admin/usuário) |
| Frontend | **Angular (standalone) + Material** | Moderno, sem boilerplate de módulos; UI consistente |
| Infra dev | **Docker (tudo em container)** | Nada instalado na máquina além do Docker; ambiente reproduzível |

> Versões: sempre a **estável madura** (nunca a recém-lançada), JDK 21 (LTS).

## 4. Como rodar (dev 100% Docker)

**Pré-requisito único no computador:** **Docker Desktop** (+ `git`). Nenhum Java/Node/Maven é instalado — tudo roda em container.

```bash
# 1. copiar variáveis de ambiente
cp .env.example .env        # e ajustar segredos (JWT_SECRET, senha do banco)

# 2. subir tudo (banco + backend + frontend)
docker compose up

# Frontend: http://localhost:4200   |   API: http://localhost:8080
```
Detalhes de config, dependências e esqueletos: **[`PLANO.md` › Apêndice C](PLANO.md)**.

## 5. Arquitetura num olhar

```
[ Angular SPA ]  --HTTP/JWT-->  [ Spring Boot API ]  -->  [ PostgreSQL ]
   (standalone)                   |  camadas: controller → service → repository
                                  |  integração externa: ViaCEP (consulta de CEP)
                                  |  fila de retry (cadastro pendente) + job agendado
                                  └  evento interno MG → lista de análise
```
- **Monorepo:** backend e frontend no mesmo repositório, em pastas separadas (`/backend`, `/frontend`).
- **Proxy de dev:** o Angular CLI proxia `/api/*` para o backend via `proxy.conf.json` — sem nginx; mesma origem para cookies `httpOnly`.
- Backend em **camadas**; regras de UF concentradas num único ponto (`finalizarCadastro`).
- Detalhe técnico completo em **[`PLANO.md`](PLANO.md)**.

---

## 6. Decisões de arquitetura (ADR)

> **Como ler:** cada decisão tem um **porquê em linguagem simples** e um **status**. Decisões mudam — quando isso acontecer, atualize o status aqui e registre na **seção 7 (histórico)** com data e motivo.
>
> **Legenda de status:** 🟢 Ativa (firme) · 🟡 Ativa (provável evolução) · 🔄 Alterada (ver histórico) · ⏳ A definir

### 6.1 Índice de decisões

| ID | Decisão | Por que (simples) | Status |
|----|---------|-------------------|--------|
| D1 | Backend em **Kotlin** | Mais conciso e seguro que Java; o enunciado permite | 🟢 |
| D2 | Evento MG via **ApplicationEvent** (interno) | Não precisa de mensageria externa para o porte atual | 🟢 |
| D3 | **PostgreSQL** em Docker | Banco real desde o início, reproduzível | 🟢 |
| D5 | **Endereço estruturado** (campos separados) | Necessário para ler a UF (regras de SP/RS/PR/MG) e o ViaCEP | 🟢 |
| D6 | Documento: **validação forte (CPF/CNPJ) em camadas** | Garante que documento inválido nunca circula; demonstra boas práticas | 🟡 |
| D7 | Migrations com **Flyway** | Histórico do banco no Git; reproduzível; seguro com vários devs | 🟢 |
| D8 | Segurança **JWT stateless** + papéis | Padrão para SPA + API; servidor não guarda sessão | 🟢 |
| D9 | **Maven** | Item obrigatório do enunciado | 🟢 |
| D10 | **JDK 21** (LTS) | Estável e suportado a longo prazo | 🟢 |
| D11 | **Monorepo** (back e front separados) | Projeto pequeno; agilidade; tudo sobe junto; bom para dev assistido por IA | 🟢 |
| D12 | ID numérico (**Long**) | Simples e performático; acesso já é protegido por login | 🟢 |
| D13 | **Exclusão lógica** (soft delete) | Nunca apaga dado; listagem mostra status ativo/inativo | 🟢 |
| D14 | Cliente ↔ UC: cascade + soft delete na UC | Remover cliente leva as UCs junto, sem perder dados | 🔄 |
| D15 | Documento **imutável** (correção só por admin) | Evita troca de identidade; corrige typo via suporte | 🟡 |
| D16 | API versionada (**/api/v1**) | Evoluir sem quebrar quem já consome | 🟢 |
| D17 | Listagem **paginada** | Escala quando a base cresce | 🟢 |
| D18 | Erros no padrão **RFC 7807** | Formato de erro padronizado, nativo do Spring | 🟢 |
| D19 | Conversão de dados **manual (Kotlin)** | Poucos objetos; evita dependência extra | 🟢 |
| D20/D21 | **ViaCEP**: front preenche, **backend confirma** | Nunca confiar no cliente para a UF que gatilha regras | 🟢 |
| — | **Fila de retry** se o ViaCEP cair | Não perde o cadastro; processa quando o serviço volta | 🟡 |
| D22 | Bloqueio SP/RS/PR retorna **422** | Status que diz "dado válido, regra recusou" | 🟢 |
| D23 | Evento MG **só registra numa lista** (por ora) | O que fazer com ela fica para depois | 🟡 |
| D24 | **Angular standalone** (≥17) | Moderno, sem módulos; menos código | 🟢 |
| D25 | Estado: **RxJS + Signals** | Cada um no que é bom; sem a complexidade do NgRx | 🟢 |
| D27 | Cadastro em **página dedicada** | Formulário grande/dinâmico não cabe bem num modal | 🟢 |
| D29 | **Tudo protegido**; admin para ações sensíveis | Segurança por padrão; inativar/corrigir só admin | 🟢 |
| D30 | Testes com **banco real efêmero** (Testcontainers) | Testa contra Postgres de verdade, não um substituto | 🟢 |
| D31 | **CI** roda lint + testes nos PRs (sem deploy) | Barra código ruim antes da `main` | 🟡 |
| D34 | **Indicador de status do ViaCEP** na tela | Transparência: usuário sabe se a consulta está no ar | 🟡 |

> Detalhe técnico e justificativa completa de **cada** decisão: **[`PLANO.md` › seção 2](PLANO.md)**.

### 6.2 Decisões em destaque (o "porquê" em prosa)

**Por que validar o documento em camadas (e não só com uma anotação)?**
Um CPF/CNPJ inválido não deveria existir em lugar nenhum do sistema. Em vez de confiar que "alguém validou na entrada", o próprio tipo `Documento` se recusa a nascer inválido. Custa um pouco mais de código, mas dá uma garantia que vale a pena — e demonstra a boa prática que o exercício avalia.

**Por que "exclusão lógica" em vez de apagar?**
Apagar perde histórico. Aqui, "remover" apenas marca como **inativo** — o dado fica para auditoria, e a tela mostra o status. A listagem mostra os ativos por padrão, com um botão para incluir inativos.

**Por que a fila quando o ViaCEP cai?**
O endereço (e a UF) é confirmado pelo backend via ViaCEP. Se essa API externa estiver fora do ar bem na hora do cadastro, em vez de recusar e fazer o usuário perder o que digitou, o cadastro entra numa **fila** e é finalizado sozinho quando o serviço volta — com acompanhamento na tela. É a funcionalidade de destaque do projeto.

**Por que "stateless" na segurança?**
O servidor não guarda sessão: cada requisição traz um token assinado que prova quem é o usuário. Isso escala melhor e combina com uma aplicação Angular consumindo a API. (O token de acesso é curto; um token de renovação, guardado de forma segura, permite continuar logado.)

**Por que monorepo (back e front separados no mesmo repositório)?**
O projeto é pequeno: não há motivo para espalhá-lo em vários repositórios. Um repositório só, com `/backend` e `/frontend` em pastas separadas, facilita rodar tudo junto (`docker compose up`), manter front e back em sincronia (commits/PRs atômicos) e desenvolver com apoio de IA (que enxerga o sistema inteiro de uma vez).

---

## 7. Histórico de mudanças de decisão

> Registre aqui toda vez que uma decisão for **alterada ou refinada** durante o desenvolvimento — com data e motivo. Mantém o "porquê" rastreável.

| Data | Decisão | Mudança | Motivo |
|------|---------|---------|--------|
| 2026-06-04 | D9 (build) | Confirmado **Maven** | Verificação no enunciado: a stack (incl. Maven) é **obrigatória**, não sugerida |
| 2026-06-04 | D10/D24 (versões) | JDK 21 e Angular ≥17 | Satisfazem o piso "17+/14+"; política de usar estável madura |
| 2026-06-04 | D15 (documento) | De "imutável" → **"imutável + correção via admin"** | Typo de documento é recorrente; tratar como caso de suporte sem abrir troca de identidade ao usuário |
| 2026-06-04 | D14 (Cliente↔UC) | De "cascade ALL" → **"cascade ALL + soft delete na UC"** | Evita que inativar um cliente apague fisicamente suas UCs (inconsistência) |
| 2026-06-04 | D21 (ViaCEP fora do ar) | De "degradar confiando na UF do cliente" → **"fila persistente + retry"** | Não perder o cadastro e nunca confiar no cliente para a UF; vira feature de destaque |
| 2026-06-04 | D18 (erros) | Confirmado **RFC 7807 (ProblemDetail)** em vez de formato custom | Usar o padrão nativo do Spring, interoperável |
| 2026-06-04 | Risco "volume de features" | Reclassificado de 🔴 (cortar) → 🟡 (**priorizar**) | Todas as features são obrigatórias; mitigação é ordem de execução, nada é cortado |
| 2026-06-05 | Proxy de dev | De **nginx reverse proxy** → **`proxy.conf.json` do Angular CLI** | Nginx era overengineering para dev: container extra, config manual de rotas, manutenção. O proxy do Angular CLI é a solução padrão documentada, zero containers extras, mesma garantia de same-origin para cookie httpOnly |
| 2026-06-05 | Swagger (C1) | De **público** → **controlado por env var** (`SPRINGDOC_PUBLIC_ACCESS`, default `false`) | Velocidade no dev local (público) com segurança em produção (protegido por padrão) |

*(novas entradas vão sendo adicionadas conforme o projeto evolui)*

---

## 8. Glossário (para qualquer leitor)

- **Unidade Consumidora (UC):** local com uma instalação (ex.: imóvel) vinculado a um cliente; um cliente pode ter várias.
- **Soft delete (exclusão lógica):** "remover" marca o registro como inativo em vez de apagar — o dado fica no banco.
- **Value Object:** um tipo que embala um valor **e** as regras que o tornam válido (ex.: `Documento`), de forma que ele nunca exista inválido.
- **Stateless:** o servidor não guarda "memória" da sua sessão; cada requisição carrega o token que prova quem você é.
- **JWT:** token assinado que contém quem é o usuário e seus papéis; o servidor só confere a assinatura.
- **Migration (Flyway):** script que versiona mudanças no banco, aplicado em ordem e registrado.
- **ViaCEP:** API pública que devolve o endereço a partir de um CEP.
- **ProblemDetail (RFC 7807):** formato padrão de resposta de erro de APIs.
- **Monorepo:** um único repositório guardando backend e frontend (em pastas separadas), em vez de um repositório para cada.
- **ADR:** *Architecture Decision Record* — registro de uma decisão de arquitetura e seu motivo.

---

## 9. Riscos conhecidos

Resumo (detalhes e mitigações em **[`PLANO.md` › 8.1](PLANO.md)**):
- **Fila assíncrona** é a parte mais complexa — duplicidade, "aceita e depois rejeita" e regra duplicada já têm mitigação desenhada.
- **Cookie de segurança em dev** exige servir front e back na mesma origem (proxy).
- **Segredos** (chave JWT, senha do admin) nunca vão para o repositório.
- **Kotlin + JPA** exige configuração específica (plugins) e entidades que não sejam `data class`.

---

> **Convenção de manutenção deste arquivo:** ao tomar/alterar uma decisão, (1) atualize o status na seção 6.1, (2) adicione uma linha na seção 7 com data e motivo, e (3) se for técnica, reflita em `PLANO.md`. Assim este README continua sendo o espelho fiel do projeto.
