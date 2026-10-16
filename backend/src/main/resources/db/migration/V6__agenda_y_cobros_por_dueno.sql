CREATE TABLE bloqueos_cancha (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 cancha_id BIGINT NOT NULL,
 fecha DATE NOT NULL,
 hora_inicio TIME NULL,
 hora_fin TIME NULL,
 motivo VARCHAR(200) NULL,
 INDEX idx_bloqueos_cancha_fecha(cancha_id,fecha),
 CONSTRAINT fk_bloqueo_cancha FOREIGN KEY(cancha_id) REFERENCES canchas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE reservas ADD COLUMN pago_requerido BIT NOT NULL DEFAULT 0,
 ADD COLUMN created_at DATETIME(6) NULL, ADD COLUMN expires_at DATETIME(6) NULL;
UPDATE reservas r JOIN pagos p ON p.reserva_id=r.id SET r.pago_requerido=1;
CREATE INDEX idx_reserva_vencimiento ON reservas(estado,expires_at);
CREATE TABLE mercadopago_cuentas (
 dueno_id BIGINT NOT NULL PRIMARY KEY,
 mp_user_id VARCHAR(80) NOT NULL UNIQUE,
 access_token VARCHAR(3000) NOT NULL,
 refresh_token VARCHAR(3000) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 live_mode BIT NOT NULL,
 CONSTRAINT fk_mp_dueno FOREIGN KEY(dueno_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE oauth_estados (
 hash VARCHAR(64) NOT NULL PRIMARY KEY,
 dueno_id BIGINT NOT NULL,
 browser_hash VARCHAR(64) NOT NULL,
 verifier VARCHAR(500) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 INDEX idx_oauth_expiracion(expires_at),
 CONSTRAINT fk_oauth_dueno FOREIGN KEY(dueno_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
