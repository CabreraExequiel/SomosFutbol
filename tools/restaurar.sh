#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
backup_dir="${1:?Uso: bash tools/restaurar.sh backups/FECHA}"
[ -s "$backup_dir/database.sql.gz" ] && [ -s "$backup_dir/uploads.tar.gz" ]
printf 'Esto reemplaza datos en la instalación actual. Escribí RESTAURAR para continuar: '
read -r confirmacion
[ "$confirmacion" = RESTAURAR ] || exit 1
# Primero hacé una copia de la instalación actual y verificá que .env usa las claves del backup.
docker compose stop backend
gzip -dc "$backup_dir/database.sql.gz" | docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'
cat "$backup_dir/uploads.tar.gz" | docker compose run --rm -T --no-deps --entrypoint tar backend -C /app/uploads -xzf -
docker compose start backend
printf 'Restauración aplicada. Verificá inicio, imágenes y reservas.\n'
