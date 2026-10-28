-- Etapa 09C5. Ejecutar UNA sola vez con Flyway o manualmente en XAMPP (nunca ambos).
-- No recrea eventos pasados: registra desde la instalacion de esta etapa en adelante.
-- Sin claves foraneas al administrador ni a la referencia: el historial permanece si se eliminan cuentas/canchas.
CREATE TABLE admin_moderacion_historial (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME(6) NOT NULL,
    referencia_tipo VARCHAR(16) NOT NULL,
    referencia_id BIGINT NOT NULL,
    accion VARCHAR(40) NOT NULL,
    estado_anterior VARCHAR(20) NOT NULL,
    estado_nuevo VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NULL,
    administrador_id BIGINT NOT NULL,
    administrador_nombre VARCHAR(150) NOT NULL,
    INDEX idx_admin_mod_ref (referencia_tipo, referencia_id, fecha, id),
    INDEX idx_admin_mod_actor (administrador_id, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
