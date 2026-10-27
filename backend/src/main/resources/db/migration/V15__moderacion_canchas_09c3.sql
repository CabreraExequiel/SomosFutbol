-- Ejecutar una sola vez en MySQL local cuando Flyway esté deshabilitado.
ALTER TABLE canchas
    ADD COLUMN motivo_desactivacion_admin VARCHAR(500) NULL,
    ADD COLUMN moderacion_admin_id BIGINT NULL,
    ADD COLUMN moderacion_admin_fecha DATETIME(6) NULL;
