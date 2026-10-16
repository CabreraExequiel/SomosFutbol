# Validación de esta entrega

Revisión realizada el 9 de octubre de 2026 (Argentina), sobre el código incluido en este proyecto.

## Resultado observado

| Comprobación | Resultado |
|---|---|
| Frontend `npm run build` de producción | Correcto, sin advertencias de presupuesto; carga inicial 348,65 kB antes de compresión |
| Backend `mvn -Djava.version=17 verify` | Correcto; 34 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Migraciones V1–V6 en base temporal vacía | Aplicadas y esquema validado al iniciar |
| Pruebas funcionales `tools/smoke.py` | 12 grupos aprobados, con API y base reales de prueba |
| Navegador Chromium | 28 comprobaciones aprobadas; escritorio 1440×1000 y móvil 390×844 |
| Errores JavaScript durante los recorridos | 0 |
| Desborde horizontal en pantallas revisadas | No detectado |
| `npm audit --omit=dev` | 0 vulnerabilidades reportadas en las dependencias de producción en esta ejecución |
| YAML de Compose y CI; sintaxis de scripts Bash/Python | Válidos |

## Flujos verificados

- Registro de jugadores/dueños con consentimiento, rechazo del registro público de administradores, sesiones y acceso por rol.
- Cancha sin agenda, publicación semanal, validación de días/rangos y superposición, bloqueo de una fecha y permisos sobre otra cancha.
- Reserva libre, rechazo de duplicados, importes exactos de media hora y liberación del turno al cancelar.
- **Dos solicitudes concurrentes al mismo turno:** una reserva aceptada y otra rechazada; no se duplicó el horario.
- Disponibilidad sin datos personales del jugador; tarjeta privada con contacto; denegación a un dueño ajeno.
- Confirmación manual por el dueño y protección de reservas confirmadas.
- Directorio de dueños, catálogo individual, pausa/republicación y detalle de una cancha pausada.
- Publicaciones, Me gusta, comentarios, notificaciones y permisos de eliminación.
- En navegador: inicio de sesión real, navegación por secciones, recorrido dueño → cancha → reserva, tarjeta del jugador con cierre por Escape y publicación de agenda desde el panel.
- Pantallas de inicio, búsqueda, complejo, cancha, comunidad, reservas, perfil, amigos, chat, notificaciones, panel, ingreso, registro y páginas legales en tamaño móvil.
- Las pruebas unitarias incluyen disponibilidad, validación de registro/cuenta, cifrado y validación de eventos de pago con proveedor simulado, incluidos pago tardío, vendedor equivocado y reembolso posterior a aprobación.

## Alcance y verificaciones pendientes

El entorno disponible tenía Java 17 y MariaDB 11.4.5. Por eso el build local usó `-Djava.version=17` y la prueba temporal sobrescribió el dialecto por `MariaDBDialect`. El proyecto entregado conserva **Java 21 y MySQL 8.4 con MySQLDialect** como destino de producción. El arranque, las migraciones y los flujos funcionaron en la base temporal; no equivale a certificar el contenedor MySQL 8.4.

La prueba de navegador sirvió el bundle compilado y redirigió `/api` al backend. No ejecutó Caddy/Nginx, emisión real de TLS ni los límites del proxy. Tampoco se ejecutaron Docker/Compose, los scripts completos de backup/restauración o PowerShell en este entorno. Se incluye `.github/workflows/verify.yml` para compilar con Java 21, probar contra MySQL 8.4 y construir ambos contenedores en un runner con Docker. Ese workflow está preparado, **no ejecutado en tu repositorio**.

No se ingresó a cuentas externas: quedan pendientes OAuth/Checkout Pro con Mercado Pago, entrega SMTP real, DNS, HTTPS del dominio y prueba de restauración en el servidor. Antes del lanzamiento, ejecutá los pasos de `DESPLIEGUE.md` y `MERCADO_PAGO.md`.

No se probaron todas las combinaciones posibles de navegadores, fallos de red, carga masiva ni migraciones de bases históricas desconocidas. Los textos legales y los datos del operador deben corresponder a tu operación real. Esta entrega no publica la aplicación por sí sola.
