-- Etapa 05A6 de Somos Futbol: indices para el feed.
-- EJECUTAR MANUALMENTE SOLO EN LA BASE LOCAL canchasya (XAMPP/MariaDB).
-- No ejecutar en Aiven ni en produccion sin autorizacion y revision previa.
-- Flyway esta desactivado; este archivo NO se aplica automaticamente.
-- Aplicar UNA sola vez; si un indice ya existe, no repetir la sentencia.
-- Antes de ejecutar: SHOW INDEX FROM publicaciones;
--
-- Soporta el orden estable del feed Recientes y el recorrido por cursor
-- (created_at DESC, id DESC), aun usando un indice ASC recorrido al reves.
CREATE INDEX idx_pub_feed_fecha_id ON publicaciones (created_at, id);

-- Soporta consultas acotadas por autor, incluido el feed Amigos.
CREATE INDEX idx_pub_feed_autor_fecha_id ON publicaciones (user_id, created_at, id);
