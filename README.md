# Somos Fútbol · versión de despliegue

Aplicación Angular 22 + API Spring Boot + MySQL. Esta entrega contiene el código actualizado y las herramientas para instalarlo en tu servidor. El dominio, los secretos y las cuentas externas se configuran en tu instalación; no se incluyen credenciales.

## Qué cambió

- **Reservas recibidas:** el dueño abre una tarjeta con nombre, foto, correo, teléfono y ciudad del jugador; cancha, fecha, horario, importe, medio de pago y estado. El servidor comprueba que la reserva le pertenece.
- **Agenda por cancha:** selección de varios días, múltiples rangos por día, publicación semanal y bloqueos para fechas particulares. Los días sin horarios quedan cerrados.
- **Buscar canchas:** directorio de todos los dueños con canchas activas, filtros y página de cada dueño con todas sus canchas. El detalle incluye agenda, horarios libres y otros espacios del mismo dueño.
- Edición de canchas, fotos, pausa y republicación; filtros de reservas y vencimiento de reservas pendientes.
- Mejoras en inicio, ingreso, registro, perfil, comunidad, amigos, mensajes y notificaciones; adaptación móvil, accesibilidad y carga de pantallas bajo demanda.
- Transferencias al dueño con confirmación manual. Integración de Mercado Pago por dueño mediante OAuth, con tokens cifrados y validación del pago en servidor.
- HTTPS con Caddy, proxy Nginx, límites de solicitudes, validación de configuración, migraciones, volúmenes persistentes y scripts de backup/restauración.

## Arranque recomendado

Usá Docker Engine / Docker Desktop con Compose v2. No necesitás instalar Java, Maven, Node ni MySQL en el equipo si usás Docker.

1. Desde la carpeta del proyecto, generá `.env` con secretos aleatorios:

   ```bash
   python tools/generar-env.py
   ```

   En Windows sin Python, usá `powershell -ExecutionPolicy Bypass -File tools/Generar-Env.ps1`. Ambos comandos conservan un `.env` existente y no muestran sus claves.

2. Para una prueba **local**, editá `.env`: `DOMAIN=localhost`, `PUBLIC_URL=http://localhost:8088`, `SPRING_PROFILES_ACTIVE=dev`, `PAYMENTS_ENABLED=false`.
3. Ejecutá:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
   ```

4. Abrí <http://localhost:8088>. Registrá un dueño, creá una cancha y publicá sus días y horarios. Con una segunda cuenta de jugador, buscá el dueño y reservá un turno.

Para producción seguí [DESPLIEGUE.md](DESPLIEGUE.md). Si ya tenés una base de datos, leé primero la sección de actualización: no borres volúmenes ni actives un baseline automático.

## Reglas de reservas

Los horarios se interpretan en `America/Argentina/Buenos_Aires`. La agenda se repite cada semana; admite varios rangos separados por día. Cada rango comienza y termina dentro del mismo día (no admite un rango que cruce medianoche). Las reservas duran entre 30 minutos y 4 horas, en intervalos de 30 minutos, hasta 90 días por adelantado. Los bloqueos puntuales pueden ser completos o parciales.

Modificar la agenda o pausar una cancha no cancela reservas ya existentes. Las reservas nuevas requieren un horario publicado, libre y no bloqueado. Una cancha sin agenda aparece en el catálogo, pero no permite reservar hasta que el dueño publique horarios. El servidor bloquea la cancha durante la operación para impedir reservas simultáneas superpuestas.

Las reservas nuevas pendientes vencen a los 15 minutos con pago online o a las 24 horas con transferencia, siempre antes de que comience el turno. La tarea de vencimiento corre cada minuto y la creación de reservas ignora pendientes ya vencidas. Las reservas anteriores a esta versión conservan su información y no reciben un vencimiento inventado.

El dueño confirma una transferencia solo después de verificarla. Las reservas confirmadas no se cancelan directamente desde la app: se coordina la devolución con el dueño. Un pago que llega después del vencimiento no recupera automáticamente el turno y genera una notificación de reintegro pendiente.

## Desarrollo sin Docker

Requisitos: Java 21, Maven 3.9+, Node 24 compatible con Angular 22 y MySQL 8.4. Creá una base vacía `canchasya` con `utf8mb4`. Configurá `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y un `JWT_SECRET` aleatorio en la terminal o IDE. El backend ejecutado directamente **no carga el `.env` de Compose**.

```bash
cd backend
mvn spring-boot:run
```

En otra terminal:

```bash
cd frontend
npm ci
npm start
```

Frontend: <http://localhost:4200>. API: <http://localhost:8080>. El proxy local redirige `/api` y evita cambiar URLs en el código. Si Maven no está instalado, podés ejecutar desde IntelliJ con su Maven integrado o usar Docker.

## Guías

- [DESPLIEGUE.md](DESPLIEGUE.md): instalación HTTPS, migración, backups y operación.
- [MERCADO_PAGO.md](MERCADO_PAGO.md): vinculación por dueño y pruebas pendientes con credenciales reales.
- [VALIDACION.md](VALIDACION.md): comprobaciones realizadas y límites de la verificación.
- [LEGAL_Y_PRIVACIDAD.md](LEGAL_Y_PRIVACIDAD.md): configuración del operador y tratamiento de datos.
- [backend/README.md](backend/README.md): API y pruebas.

No se incluyen dependencias descargadas ni compilaciones anteriores. `npm ci` y las imágenes de Docker reconstruyen el proyecto desde sus archivos de dependencias.
