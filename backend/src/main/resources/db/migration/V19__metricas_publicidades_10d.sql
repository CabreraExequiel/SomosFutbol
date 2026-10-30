-- ETAPA 10D. Ejecutar UNA sola vez en MySQL LOCAL (o dejar que Flyway lo ejecute).
-- NO ejecutar en Aiven durante las pruebas locales.
-- Contadores agregados por día; sin IP, correo ni identidad de los visitantes.
CREATE TABLE publicidad_metricas_diarias (
    publicidad_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    ubicacion VARCHAR(24) NOT NULL,
    impresiones BIGINT NOT NULL DEFAULT 0,
    clics BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (publicidad_id, fecha, ubicacion),
    INDEX idx_publicidad_metricas_fecha (fecha, ubicacion),
    CONSTRAINT fk_publicidad_metricas_publicidad
      FOREIGN KEY (publicidad_id) REFERENCES publicidades(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
