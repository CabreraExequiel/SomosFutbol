-- ETAPA 10A: correr UNA SOLA VEZ en MySQL local (o dejar que Flyway la ejecute), nunca ambas.
-- NO ejecutar en Aiven durante las pruebas locales.
CREATE TABLE publicidades (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(400) NULL,
    imagen_url VARCHAR(700) NULL,
    destino_url VARCHAR(700) NOT NULL,
    ubicacion VARCHAR(24) NOT NULL,
    estado VARCHAR(16) NOT NULL DEFAULT 'BORRADOR',
    prioridad INT NOT NULL DEFAULT 0,
    fecha_inicio DATETIME(6) NULL,
    fecha_fin DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    creado_por_id BIGINT NOT NULL,
    actualizado_por_id BIGINT NOT NULL,
    INDEX idx_publicidad_estado_ubicacion (estado, ubicacion),
    INDEX idx_publicidad_ventana (fecha_inicio, fecha_fin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
