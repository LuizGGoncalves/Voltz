ALTER TABLE cadastro_pendente
    ADD COLUMN cliente_id BIGINT REFERENCES cliente(id);
