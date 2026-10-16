CREATE TABLE IF NOT EXISTS publicacion_menciones (
    publicacion_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (publicacion_id, user_id),
    CONSTRAINT fk_menciones_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_menciones_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
