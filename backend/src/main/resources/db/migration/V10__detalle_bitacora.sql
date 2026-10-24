-- Etapa 3C: información estructurada del origen de errores.
-- Ejecutar SOLO UNA VEZ en canchasya LOCAL, después de respaldar.
-- No modifica ni elimina entradas actuales; nuevos campos NULL para registros históricos.
ALTER TABLE bitacora_eventos
  ADD COLUMN clase_origen VARCHAR(180) NULL,
  ADD COLUMN funcion_origen VARCHAR(120) NULL,
  ADD COLUMN archivo_origen VARCHAR(150) NULL,
  ADD COLUMN linea_origen INT NULL,
  ADD COLUMN causa_probable VARCHAR(240) NULL,
  ADD COLUMN sql_state VARCHAR(5) NULL,
  ADD COLUMN codigo_sql INT NULL;
