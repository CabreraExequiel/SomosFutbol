# API de Somos Fútbol

Spring Boot, Security, JPA, MySQL, Flyway y JWT. Inicio y configuración completos en [README principal](../README.md).

```bash
mvn verify
mvn spring-boot:run
```

El proyecto declara Java 21. El perfil `prod` valida HTTPS, secreto JWT, configuración de base e identidad del operador. No se activa automáticamente: usá `SPRING_PROFILES_ACTIVE=prod` en producción. Hibernate valida el esquema; Flyway aplica V1–V6. Nunca se habilita un baseline implícito.

## API principal

| Área | Rutas |
|---|---|
| Autenticación | `POST /api/auth/register`, `POST /api/auth/login` |
| Perfil | `GET/PUT /api/users/me`, avatar y cuenta de cobro privada |
| Dueños publicados | `GET /api/canchas/duenos`, `GET /api/canchas/duenos/{id}` |
| Canchas | `GET/POST /api/canchas`, `GET/PUT /api/canchas/{id}`, `PATCH /api/canchas/{id}/publicacion` |
| Agenda semanal | `GET/PUT /api/canchas/{id}/horarios`; PUT reemplaza atómicamente `{horarios:[{diaSemana:1,horaInicio:"18:00",horaFin:"23:00"}]}` |
| Bloqueos | `GET/POST /api/canchas/{id}/bloqueos`, `DELETE /api/canchas/{id}/bloqueos/{bloqueoId}` |
| Disponibilidad | `GET /api/reservas/disponibilidad?canchaId=ID&fecha=AAAA-MM-DD` (autenticado, solo rangos) |
| Reservas | `GET/POST /api/reservas`, `GET /api/reservas/recibidas`, confirmación/cancelación |
| Detalle privado | `GET /api/reservas/{id}/detalle`, `GET /api/reservas/{id}/datos-transferencia` |
| Mercado Pago | `GET /api/mercadopago/estado`, `POST /api/mercadopago/vincular`, callback y `/api/pagos/webhook` |
| Comunidad | `/api/publicaciones`, `/api/amistades`, `/api/chat`, `/api/notificaciones` |
| Configuración pública | `GET /api/config` (datos del operador y versiones) |
| Archivos | `GET /api/media/{categoria}/{archivo}` |

Los días se representan de 0=domingo a 6=sábado. Las rutas privadas requieren Bearer JWT. La API vuelve a comprobar roles y propiedad en cada operación; el estado del navegador no concede permisos.

## Prueba funcional

Con una API y base **locales y descartables**, desde la raíz del proyecto:

```bash
SF_TEST_API=http://127.0.0.1:8080/api python3 tools/smoke.py
```

El script crea jugadores, dueños, canchas, reservas y publicaciones; conserva esos datos para inspección. Comprueba permisos, agenda, bloqueos, privacidad y dos solicitudes concurrentes al mismo horario. Nunca se debe ejecutar sobre una base de producción. Su destino se limita a localhost.

## Archivos y operación

JPG/PNG hasta 5 MB, máximo 20 millones de píxeles y 8000 por dimensión, con contenido verificado antes de persistir. Las URLs nuevas son relativas a `/api/media`, los nombres se generan y el directorio se normaliza. El volumen `UPLOAD_DIR` necesita backup junto a la base.

El correo es opcional; con `MAIL_ENABLED=false` no bloquea el chequeo de salud. El pago online se configura por dueño; ver [MERCADO_PAGO.md](../MERCADO_PAGO.md).
