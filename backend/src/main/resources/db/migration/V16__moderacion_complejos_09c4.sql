-- Etapa 09C4: suspensión del complejo por propietario.
-- Los estados individuales de canchas.activa permanecen intactos.
-- Aplicar una sola vez (Flyway O manualmente en MySQL local, nunca ambos).
ALTER TABLE users
    ADD COLUMN complejo_suspendido TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN complejo_motivo_suspension VARCHAR(500) NULL,
    ADD COLUMN complejo_suspendido_en DATETIME(6) NULL,
    ADD COLUMN complejo_suspendido_por_id BIGINT NULL;
