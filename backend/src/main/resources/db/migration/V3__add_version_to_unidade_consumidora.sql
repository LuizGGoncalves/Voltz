-- V3 — Adicionar version na unidade_consumidora para optimistic locking
-- Necessário para endpoints independentes de UC (edição sem passar pelo Cliente)
ALTER TABLE unidade_consumidora ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
