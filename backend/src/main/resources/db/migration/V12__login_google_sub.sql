-- Ejecución local una sola vez, después de V11. Conserva cuentas tradicionales.
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255) NULL;
CREATE UNIQUE INDEX uk_users_google_sub ON users (google_sub);
