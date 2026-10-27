-- Ejecutar una sola vez en MySQL local si Flyway está deshabilitado.
ALTER TABLE users
  ADD COLUMN suspendido BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN motivo_suspension VARCHAR(500) NULL,
  ADD COLUMN suspendido_en DATETIME(6) NULL,
  ADD COLUMN suspendido_por_id BIGINT NULL;
