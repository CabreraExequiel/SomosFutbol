CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefono VARCHAR(30) NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    ciudad VARCHAR(100) NULL,
    avatar VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ciudades (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    provincia VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ciudades_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS canchas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ciudad_id BIGINT NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    direccion VARCHAR(255) NULL,
    tipo VARCHAR(80) NULL,
    superficie VARCHAR(80) NULL,
    cant_jugadores INT NULL,
    precio_hora DECIMAL(12,2) NOT NULL,
    techada BIT NOT NULL DEFAULT 0,
    imagen VARCHAR(500) NULL,
    descripcion VARCHAR(2000) NULL,
    activa BIT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_canchas_user (user_id),
    INDEX idx_canchas_ciudad_activa (ciudad_id, activa),
    CONSTRAINT fk_canchas_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_canchas_ciudad FOREIGN KEY (ciudad_id) REFERENCES ciudades (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS horarios_disponibles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cancha_id BIGINT NOT NULL,
    dia_semana INT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_horarios_cancha_dia (cancha_id, dia_semana),
    CONSTRAINT fk_horarios_cancha FOREIGN KEY (cancha_id) REFERENCES canchas (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reservas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    cancha_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    estado VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_reservas_user_fecha (user_id, fecha),
    INDEX idx_reservas_cancha_fecha (cancha_id, fecha, estado),
    CONSTRAINT fk_reservas_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservas_cancha FOREIGN KEY (cancha_id) REFERENCES canchas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS publicaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    contenido VARCHAR(2000) NOT NULL,
    imagen VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_publicaciones_fecha (created_at),
    CONSTRAINT fk_publicaciones_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comentarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    contenido VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_comentarios_publicacion (publicacion_id, created_at),
    CONSTRAINT fk_comentarios_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_comentarios_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS likes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_likes_publicacion_user UNIQUE (publicacion_id, user_id),
    CONSTRAINT fk_likes_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_uno_id BIGINT NOT NULL,
    user_dos_id BIGINT NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_conversaciones_usuarios UNIQUE (user_uno_id, user_dos_id),
    CONSTRAINT fk_conversaciones_user_uno FOREIGN KEY (user_uno_id) REFERENCES users (id),
    CONSTRAINT fk_conversaciones_user_dos FOREIGN KEY (user_dos_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mensajes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversacion_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    contenido VARCHAR(2000) NOT NULL,
    leido BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_mensajes_conversacion_fecha (conversacion_id, created_at),
    CONSTRAINT fk_mensajes_conversacion FOREIGN KEY (conversacion_id) REFERENCES conversaciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_mensajes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pagos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reserva_id BIGINT NOT NULL,
    mp_payment_id VARCHAR(255) NULL,
    mp_preference_id VARCHAR(255) NULL,
    monto DECIMAL(12,2) NOT NULL,
    estado VARCHAR(32) NOT NULL,
    metodo VARCHAR(100) NULL,
    fecha_pago DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_pagos_reserva UNIQUE (reserva_id),
    CONSTRAINT fk_pagos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS amistades (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pair_key VARCHAR(50) NOT NULL,
    solicitante_id BIGINT NOT NULL,
    destinatario_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    responded_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_amistad_pair UNIQUE (pair_key),
    INDEX idx_amistad_solicitante (solicitante_id, estado),
    INDEX idx_amistad_destinatario (destinatario_id, estado),
    CONSTRAINT fk_amistad_solicitante FOREIGN KEY (solicitante_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_amistad_destinatario FOREIGN KEY (destinatario_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    tipo VARCHAR(30) NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    destino VARCHAR(300) NULL,
    referencia_id BIGINT NULL,
    leida BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_notificacion_user_fecha (user_id, created_at),
    INDEX idx_notificacion_user_leida (user_id, leida),
    CONSTRAINT fk_notificacion_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
