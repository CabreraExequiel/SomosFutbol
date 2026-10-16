# Instalación y actualización

## Instalación nueva en un servidor

1. Prepará un servidor Linux con Docker y Compose v2. Apuntá el registro DNS A del dominio al servidor. Si hay un registro AAAA, también debe apuntar al servidor correcto. Permití tráfico entrante 80 y 443; MySQL y la API no necesitan puertos públicos.
2. Copiá el proyecto y ejecutá `python3 tools/generar-env.py`. Conservá el archivo `.env` en privado.
3. Completá `DOMAIN` (sin protocolo), `PUBLIC_URL=https://TU_DOMINIO`, `SPRING_PROFILES_ACTIVE=prod`, `LEGAL_OPERATOR`, `LEGAL_TAX_ID`, `LEGAL_ADDRESS` y `SUPPORT_EMAIL`. El arranque productivo rechaza los valores vacíos y el dominio de ejemplo.
4. Conservá inicialmente `PAYMENTS_ENABLED=false` y `MAIL_ENABLED=false`. Cada dueño puede recibir transferencias en su propia cuenta. Activá servicios externos solo después de sus pruebas.
5. Construí e iniciá:

   ```bash
   docker compose config --quiet
   docker compose up -d --build
   docker compose ps
   docker compose logs --tail=100 backend gateway
   ```

6. Abrí tu dominio HTTPS. Caddy solicita y renueva el certificado; sus volúmenes deben persistir. Probá registro, inicio de sesión, foto, publicación, agenda, reserva, detalle del jugador y confirmación con cuentas de prueba. El endpoint `/healthz` confirma que responde Nginx; el estado de la API se verifica con `docker compose exec backend curl -fsS http://localhost:8080/actuator/health`.
7. Configurá monitorización externa, una copia de seguridad diaria y alertas de errores. Ejecutá una restauración en una instalación descartable antes de depender de los backups.

No expongas puertos de MySQL/backend ni uses el override local en producción. El despliegue incluido está pensado para **un servidor**. Varias réplicas necesitan un almacenamiento de imágenes compartido, revisión del planificador y de los límites por IP.

## Variables

| Variable | Valor / finalidad |
|---|---|
| `DOMAIN`, `PUBLIC_URL` | Dominio público y origen HTTPS único, sin ruta final |
| `DB_PASSWORD`, `DB_ROOT_PASSWORD` | Claves distintas generadas para MySQL; cambiar `.env` no modifica contraseñas de un volumen ya inicializado |
| `JWT_SECRET` | Secreto aleatorio de al menos 48 caracteres; rotarlo cierra las sesiones existentes |
| `JWT_EXPIRATION_MS` | 43.200.000 por defecto en Compose (12 horas) |
| `LEGAL_OPERATOR`, `LEGAL_TAX_ID`, `LEGAL_ADDRESS`, `SUPPORT_EMAIL` | Identidad y contacto visibles en las páginas legales |
| `TERMS_VERSION`, `PRIVACY_VERSION` | Versiones de consentimiento; por defecto 2026-10-09 |
| `PAYMENTS_ENABLED` | `false`: transferencia manual; `true`: Checkout Pro por dueño |
| `MERCADOPAGO_TEST_MODE` | `true` para pruebas, `false` para cuentas reales |
| `MERCADOPAGO_CLIENT_ID`, `MERCADOPAGO_CLIENT_SECRET`, `MERCADOPAGO_WEBHOOK_SECRET` | Credenciales de la aplicación de Mercado Pago |
| `MP_ENCRYPTION_KEY` | Clave Base64 de 32 bytes para tokens OAuth; conservar junto al backup |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP opcional para tickets de pagos aprobados |

La clave de cifrado no se rota cambiando solamente `.env`: eso impediría leer los tokens guardados. Conservá la clave mientras existan cuentas vinculadas; una rotación requiere migrar el cifrado de esos datos.

## Actualizar una instalación existente

1. Conservá la versión anterior del código, un volcado de la base, las imágenes originales y la configuración. Detené las escrituras durante la copia. **No uses `docker compose down -v`**: elimina volúmenes.
2. Restaurá una copia de la base y los archivos en un entorno de prueba. Si usás Docker desde una instalación anterior, verificá los nombres reales de sus volúmenes: este Compose utiliza el proyecto `somos-futbol`. Un cambio de nombre puede crear volúmenes nuevos vacíos; restaurá la copia deliberadamente.
3. Consultá `flyway_schema_history`. En la versión anterior con V1–V5 registradas, Flyway verifica los checksums y aplica V6 al iniciar. V1–V5 no se modificaron. V6 añade agenda bloqueada, campos de vencimiento y modo de pago, cuentas OAuth y estados temporales.
4. Si la base tiene tablas pero **no tiene historial de Flyway**, el inicio se detiene deliberadamente. Compará el esquema real con V1–V5 y reconciliá sus diferencias en la copia antes de registrar un baseline. No habilites `baseline-on-migrate` a ciegas, no borres el historial y no uses `ddl-auto=update` para ocultar diferencias.
5. Publicá los días y horarios de las canchas existentes. No se inventan horarios de atención. Revisá las reservas pendientes antiguas: conservan su estado y no reciben un vencimiento retroactivo.
6. Si las fotos guardadas apuntan a localhost o ngrok, copiá primero la carpeta original `uploads` al volumen nuevo. Revisá `tools/migrar-imagenes.sql` y ejecutalo en la copia para convertir esas URLs a rutas `/api/media/...`. Verificá varias fotos antes de repetir en producción.
7. Si existieron pagos con el token global de la versión anterior, conciliá esos pagos y checkouts pendientes con esa cuenta antes del cambio. Esta versión usa credenciales del dueño; no puede consultar pagos históricos de una cuenta global usando tokens de otros vendedores. No cambies credenciales con pagos pendientes sin una conciliación.
8. Repetí las pruebas funcionales sobre la copia. Después actualizá producción con los backups ya verificados y ejecutá `docker compose up -d --build`.

Una reversión después de V6 requiere restaurar la base y el código que corresponden al mismo backup; no basta con volver al código anterior. Restaurar una copia puede perder actividad posterior, por lo que primero se debe conciliar esa actividad.

## Backups

En Linux, desde la raíz del proyecto:

```bash
bash tools/backup.sh
```

El script pausa brevemente el backend, exporta la base, las imágenes y `.env`, y reinicia el backend al terminar. Los archivos quedan en `backups/FECHA`; contienen datos personales y secretos. Guardá una copia cifrada fuera del servidor, limitá acceso y definí una retención acorde a tu operación. Programá el script con un usuario autorizado de Docker y verificá que termine con código cero.

Para restaurar **en una instalación preparada y con las claves del backup**:

```bash
bash tools/restaurar.sh backups/FECHA
```

El script pide escribir `RESTAURAR`. Antes hacé un backup del estado actual. No sustituye automáticamente `.env`: verificá las credenciales de la base y, especialmente, `MP_ENCRYPTION_KEY`. Si una importación falla, el backend queda detenido para evitar nuevas escrituras sobre un estado incompleto.

## Límites de esta entrega

El proyecto no incluye recuperación de contraseña por correo, verificación de email, moderación administrativa completa ni reembolsos automáticos desde la app. Las devoluciones se gestionan con el dueño en Mercado Pago o por transferencia, según corresponda. La mensajería consulta periódicamente, sin WebSocket. Las actualizaciones materiales de textos legales necesitan un flujo de reaceptación para usuarios anteriores.

La instalación Docker/Java 21/MySQL 8.4 y la operación con servicios externos deben validarse en tu servidor antes de abrirlo al público. Consultá el alcance exacto de las pruebas en `VALIDACION.md`.
