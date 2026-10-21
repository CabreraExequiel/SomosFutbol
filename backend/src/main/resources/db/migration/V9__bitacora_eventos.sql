-- Etapa 3A. Ejecutar solo en la base LOCAL, una vez y con respaldo previo.
-- Flyway está desactivado en la configuración actual: ejecutar manualmente con phpMyAdmin.
-- No modifica datos existentes ni contiene usuarios, contraseñas o tokens.
CREATE TABLE IF NOT EXISTS bitacora_eventos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  fecha DATETIME(6) NOT NULL,
  nivel VARCHAR(16) NOT NULL,
  origen VARCHAR(20) NOT NULL,
  ambiente VARCHAR(32) NOT NULL,
  tipo VARCHAR(64) NOT NULL,
  codigo VARCHAR(48) NOT NULL,
  descripcion VARCHAR(240) NOT NULL,
  actor_id BIGINT NULL,
  referencia_tipo VARCHAR(32) NULL,
  referencia_id BIGINT NULL,
  ruta VARCHAR(180) NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_bitacora_codigo(codigo),
  INDEX idx_bitacora_fecha(fecha),
  INDEX idx_bitacora_nivel_fecha(nivel, fecha),
  INDEX idx_bitacora_origen_fecha(origen, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
