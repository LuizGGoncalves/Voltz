-- V4 — Tabela de auditoria para correção de documento
CREATE TABLE auditoria_documento (
    id              BIGSERIAL    PRIMARY KEY,
    cliente_id      BIGINT       NOT NULL REFERENCES cliente(id),
    documento_anterior VARCHAR(14) NOT NULL,
    documento_novo  VARCHAR(14)  NOT NULL,
    motivo          VARCHAR(500) NOT NULL,
    usuario         VARCHAR(150) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
