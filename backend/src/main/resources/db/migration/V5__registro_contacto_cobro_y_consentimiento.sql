ALTER TABLE users
    MODIFY COLUMN telefono VARCHAR(30) NULL,
    ADD COLUMN cbu_cvu VARCHAR(22) NULL AFTER avatar,
    ADD COLUMN alias_mercado_pago VARCHAR(20) NULL AFTER cbu_cvu,
    ADD COLUMN terms_accepted_at DATETIME(6) NULL AFTER alias_mercado_pago,
    ADD COLUMN terms_version VARCHAR(30) NULL AFTER terms_accepted_at,
    ADD COLUMN privacy_accepted_at DATETIME(6) NULL AFTER terms_version,
    ADD COLUMN privacy_version VARCHAR(30) NULL AFTER privacy_accepted_at;
