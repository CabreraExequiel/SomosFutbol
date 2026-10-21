-- Etapa 1b: permite que el propietario defina el nombre comercial del complejo.
-- Migrar manualmente una sola vez en LOCAL con backup, ya que Flyway está desactivado.
ALTER TABLE users ADD COLUMN nombre_complejo VARCHAR(150) NULL;
