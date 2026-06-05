# QA Visual — Checklist de Teste no Browser

> **Como usar:** suba o app (`docker compose up -d --build`), abra `http://localhost:4200` e valide cada item.

---

## 1. Login (`/login`)

- [ ] Tela centralizada com card branco
- [ ] Campos "Usuário" e "Senha" com Material outline
- [ ] Login com `admin` / `admin123` → redireciona para `/clientes`
- [ ] Login com senha errada → mensagem "Usuário ou senha inválidos"
- [ ] Spinner aparece durante o login
- [ ] Botão desabilitado durante loading

## 2. Layout (após login)

- [ ] Sidenav à esquerda com links: Clientes, Pendentes, Análises MG
- [ ] Toolbar no topo com nome do usuário (`admin`) e botão logout
- [ ] Badge ViaCEP no rodapé do sidenav (verde "disponível")
- [ ] Logout redireciona para `/login`

## 3. Listagem de Clientes (`/clientes`)

- [ ] Tabela com colunas: Nome, Documento, Status, Criado em, Ações
- [ ] Botão "Novo Cliente" visível
- [ ] Toggle "Incluir inativos" funciona
- [ ] Documento formatado com máscara (CPF: 000.000.000-00)
- [ ] Badge "Ativo" verde / "Inativo" vermelho
- [ ] Paginação funciona
- [ ] Ícone de olho (ver detalhe) e lápis (editar) nas ações

## 4. Cadastro de Cliente (`/clientes/novo`)

- [ ] Formulário: Nome, CPF/CNPJ (com máscara), Endereço
- [ ] Ao digitar CEP e sair do campo → autofill (logradouro, bairro, cidade, UF preenchidos e read-only)
- [ ] Seção "Unidade Consumidora Inicial" visível
- [ ] UC também tem autofill de CEP
- [ ] Validação: campos obrigatórios destacados ao submeter vazio
- [ ] Criar com CEP de MG (30140-071) → sucesso + snackbar "Cliente criado!"
- [ ] Redireciona para `/clientes`

## 5. Detalhe do Cliente (`/clientes/:id`)

- [ ] Dados do cliente exibidos (nome, documento, endereço, status)
- [ ] Tabela de UCs com colunas: Nome, Nº Instalação, Endereço, Ações
- [ ] Botão "Nova UC" abre dialog modal
- [ ] Botão editar UC abre dialog com dados preenchidos
- [ ] Botão remover UC pede confirmação
- [ ] Botão "Editar Cliente" leva para formulário de edição

## 6. Dialog de UC (modal)

- [ ] Campos: Nome, Nº Instalação, CEP (com máscara), Número, Complemento
- [ ] Autofill de CEP funciona
- [ ] Campos de endereço ficam read-only após autofill
- [ ] Salvar → snackbar + dialog fecha + lista atualiza
- [ ] Tentar UC em SP (CEP 01001-000) → erro "SP não é permitida"

## 7. Edição de Cliente (`/clientes/:id/editar`)

- [ ] Formulário vem preenchido com dados atuais
- [ ] SEM seção de UCs (UCs gerenciadas no detalhe)
- [ ] Alterar nome e salvar → snackbar "Cliente atualizado!"

## 8. Pendentes (`/pendentes`)

- [ ] Filtro por status (Todos, Pendente, Processado, Rejeitado, Falha)
- [ ] Tabela com colunas: ID, Documento, Status, Motivo, Tentativas, Criado em
- [ ] Badges coloridos por status
- [ ] Paginação funciona

## 9. Análises MG (`/analises-mg`)

- [ ] Texto explicativo sobre análises MG
- [ ] Tabela com: ID, Cliente ID, UC ID, Status, Registrado em
- [ ] Badge "PENDENTE_ANALISE"
- [ ] Paginação funciona

## 10. Responsividade

- [ ] Sidenav não colapsa em tela grande
- [ ] Formulários se adaptam em tela média (flex-wrap)
- [ ] Tabelas permitem scroll horizontal em tela pequena

## 11. Fluxos E2E

- [ ] **Criar → Editar → Inativar:** criar cliente, editar nome, inativar via listagem
- [ ] **Criar → Adicionar UC → Editar UC → Remover UC:** fluxo completo de UCs no detalhe
- [ ] **Recarregar página:** não perde sessão (guard faz refresh do token)
- [ ] **Login → Navegar → Logout → Tentar acessar /clientes:** redireciona ao login

---

> Registre falhas encontradas abaixo com screenshot/descrição:

## Falhas Encontradas

| # | Tela | Descrição | Severidade |
|---|------|-----------|-----------|
| | | | |
