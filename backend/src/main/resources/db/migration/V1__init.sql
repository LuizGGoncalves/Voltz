-- =============================================
-- V1 — Schema inicial: todas as tabelas do MVP
-- =============================================

-- === Segurança ===

CREATE TABLE role (
    id   BIGSERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE usuario (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(150) NOT NULL UNIQUE,
    senha    VARCHAR(255) NOT NULL,
    ativo    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario_role (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    role_id    BIGINT NOT NULL REFERENCES role(id),
    PRIMARY KEY (usuario_id, role_id)
);

CREATE TABLE refresh_token (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuario(id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expira_em  TIMESTAMPTZ  NOT NULL,
    revogado   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- === Domínio ===

CREATE TABLE cliente (
    id              BIGSERIAL    PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    documento       VARCHAR(14)  NOT NULL,
    -- endereco (embeddable)
    endereco_cep         VARCHAR(8)   NOT NULL,
    endereco_logradouro  VARCHAR(255) NOT NULL,
    endereco_numero      VARCHAR(20)  NOT NULL,
    endereco_complemento VARCHAR(100),
    endereco_bairro      VARCHAR(100) NOT NULL,
    endereco_cidade      VARCHAR(100) NOT NULL,
    endereco_uf          VARCHAR(2)   NOT NULL,
    --
    ativo      BOOLEAN     NOT NULL DEFAULT TRUE,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Índice único parcial: documento único entre ativos (soft delete)
CREATE UNIQUE INDEX uk_cliente_documento_ativo ON cliente (documento) WHERE ativo = TRUE;

CREATE TABLE unidade_consumidora (
    id                   BIGSERIAL    PRIMARY KEY,
    nome                 VARCHAR(255) NOT NULL,
    numero_instalacao    VARCHAR(50)  NOT NULL,
    -- endereco (embeddable)
    endereco_cep         VARCHAR(8)   NOT NULL,
    endereco_logradouro  VARCHAR(255) NOT NULL,
    endereco_numero      VARCHAR(20)  NOT NULL,
    endereco_complemento VARCHAR(100),
    endereco_bairro      VARCHAR(100) NOT NULL,
    endereco_cidade      VARCHAR(100) NOT NULL,
    endereco_uf          VARCHAR(2)   NOT NULL,
    --
    cliente_id BIGINT  NOT NULL REFERENCES cliente(id),
    ativo      BOOLEAN NOT NULL DEFAULT TRUE
);

-- Índice único parcial: instalação única entre ativas
CREATE UNIQUE INDEX uk_uc_numero_instalacao_ativo ON unidade_consumidora (numero_instalacao) WHERE ativo = TRUE;

-- === Fila de retry (cadastro pendente) ===

CREATE TABLE cadastro_pendente (
    id              BIGSERIAL    PRIMARY KEY,
    documento       VARCHAR(14)  NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    motivo          VARCHAR(500),
    tentativas      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultima_tentativa TIMESTAMPTZ
);

-- Anti-duplicidade na fila
CREATE UNIQUE INDEX uk_pendente_documento ON cadastro_pendente (documento) WHERE status = 'PENDENTE';

-- === Evento MG ===

CREATE TABLE analise_cliente_mg (
    id                      BIGSERIAL   PRIMARY KEY,
    cliente_id              BIGINT      NOT NULL REFERENCES cliente(id),
    unidade_consumidora_id  BIGINT      NOT NULL REFERENCES unidade_consumidora(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDENTE_ANALISE',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- === Seed de dados iniciais ===

INSERT INTO role (nome) VALUES ('ADMIN'), ('USER');

INSERT INTO usuario (username, senha)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');
-- senha: admin123 (BCrypt) — trocar em produção

INSERT INTO usuario_role (usuario_id, role_id)
SELECT u.id, r.id FROM usuario u, role r WHERE u.username = 'admin' AND r.nome = 'ADMIN';

INSERT INTO usuario_role (usuario_id, role_id)
SELECT u.id, r.id FROM usuario u, role r WHERE u.username = 'admin' AND r.nome = 'USER';
