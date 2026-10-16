# Mercado Pago por dueño

## Dos modos de cobro

Con `PAYMENTS_ENABLED=false`, cada jugador puede consultar el alias/CBU/CVU del dueño **solo desde una reserva propia**. El dueño verifica el ingreso en su cuenta y confirma manualmente. Guardar un alias no valida una transferencia.

Con `PAYMENTS_ENABLED=true`, cada dueño vincula su propia cuenta mediante OAuth desde Perfil. La preferencia de Checkout Pro usa el Access Token cifrado de ese dueño, por lo que el vendedor es el dueño vinculado. Esta implementación no añade comisión de la plataforma ni incluye reembolsos automáticos.

No se utiliza `MERCADOPAGO_ACCESS_TOKEN` global. Las reservas conservan su modo de pago al crearse; cambiar la variable no convierte reservas anteriores.

## Configuración y prueba

1. Usá una instalación HTTPS separada y una base de prueba. Configurá una aplicación de Mercado Pago con OAuth y Checkout Pro según los permisos y requisitos que Mercado Pago habilite para tu cuenta.
2. Registrá como redirect URI exacta `https://TU_DOMINIO/api/mercadopago/callback`.
3. Configurá las notificaciones de pagos y el secreto de firma. El endpoint es `https://TU_DOMINIO/api/pagos/webhook`. Las preferencias añaden `seller_id` al callback para seleccionar las credenciales del vendedor; ese parámetro es solo una pista de enrutamiento, nunca una prueba de identidad.
4. Configurá `MERCADOPAGO_CLIENT_ID`, `MERCADOPAGO_CLIENT_SECRET`, `MERCADOPAGO_WEBHOOK_SECRET`, `MP_ENCRYPTION_KEY`, `PAYMENTS_ENABLED=true` y `MERCADOPAGO_TEST_MODE=true` con la modalidad de prueba autorizada para tu aplicación. No mezcles compradores/vendedores ni credenciales de prueba con producción.
5. Desde Perfil, un dueño elige **Vincular Mercado Pago**, autoriza la cuenta y vuelve a la app. El sistema verifica estado aleatorio, navegador, vencimiento y PKCE. La misma cuenta vendedora no puede asignarse a dos dueños. La revinculación debe conservar el vendedor para mantener la trazabilidad.
6. Con otro usuario, reservá, abrí Checkout Pro y completá un pago de prueba. La pantalla de retorno no confirma el pago: lo confirma la API al verificar la notificación y consultar Mercado Pago.
7. Verificá pago aprobado, rechazado, pendiente, cancelado, webhook repetido y reembolso. Verificá además dos dueños distintos: cada uno debe recibir su propio cobro.
8. Comprobá que un importe, moneda, vendedor, preferencia o entorno diferente no confirme la reserva; que un pago tardío no ocupe un turno ya liberado; y que las notificaciones lleguen al dueño y al jugador correctos.
9. Para producción usá la configuración real en una base de producción, `MERCADOPAGO_TEST_MODE=false` y cuentas reales vinculadas. Hacé una operación controlada de bajo importe y su devolución antes del lanzamiento. No reutilices las cuentas OAuth de la base de prueba.

## Protecciones implementadas

- Tokens y verificador PKCE cifrados con AES-256-GCM en la base. La clave vive fuera del código.
- Cookie de vinculación `HttpOnly`, `Secure`, `SameSite=Lax`; estado de un solo uso con vencimiento de 10 minutos.
- Renovación del token del vendedor antes de vencer, con bloqueo de su registro.
- Firma de webhook y consulta del pago al proveedor; comprobación de vendedor, identificadores, importe exacto, moneda, metadata, preferencia y entorno.
- Idempotencia de notificaciones y manejo de reembolsos posteriores a una aprobación.
- Expiración de reservas nuevas con checkout abandonado. Los pagos tardíos requieren conciliación y devolución por el dueño.

Si un jugador obtiene un error al pagar, revisá la vinculación del dueño y los logs del backend. Nunca le pidas que envíe tokens o secretos. Una cuenta revocada debe volver a autorizarse desde Perfil.

## Pagos de versiones anteriores

La versión anterior utilizaba un vendedor global. Sus pagos no se transfieren de cuenta al actualizar. Conciliá esos pagos y checkouts pendientes antes de habilitar cobros por dueño. Conservá la evidencia necesaria para devolver dinero desde la cuenta que lo recibió.

## Alcance de la validación

Se probaron automáticamente reglas y eventos de pago con respuestas simuladas del proveedor. **No se realizó una autorización ni un cobro real con Mercado Pago**, porque no se proporcionaron credenciales ni un dominio de la instalación. La guía anterior es una condición de puesta en marcha, no una afirmación de certificación del proveedor.

Referencias oficiales: [OAuth](https://www.mercadopago.com.ar/developers/es/reference/authentication/oauth/_oauth_token/post) y [documentación de Mercado Pago](https://www.mercadopago.com.ar/developers/es/docs).
