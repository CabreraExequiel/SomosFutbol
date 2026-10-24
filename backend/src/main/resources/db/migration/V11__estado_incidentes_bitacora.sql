-- Etapa 3D: seguimiento de incidentes. Ejecutar una sola vez en canchasya LOCAL.
-- Primero exportar respaldo. No eliminar datos históricos.
ALTER TABLE bitacora_eventos
  ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  ADD COLUMN atendido_por_id BIGINT NULL,
  ADD COLUMN atendido_en DATETIME(6) NULL,
  ADD INDEX idx_bitacora_estado_fecha (estado, fecha);
