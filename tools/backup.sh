#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
backup_dir="backups/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$backup_dir"
# Pausar escrituras brevemente para obtener una copia consistente de BD + imágenes.
docker compose stop backend
trap 'docker compose start backend >/dev/null' EXIT
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces "$MYSQL_DATABASE"' | gzip > "$backup_dir/database.sql.gz"
docker compose run --rm --no-deps --entrypoint tar backend -C /app/uploads -czf - . > "$backup_dir/uploads.tar.gz"
cp .env "$backup_dir/config.env"
printf 'Backup creado en %s. Guardalo cifrado fuera del servidor.\n' "$backup_dir"
