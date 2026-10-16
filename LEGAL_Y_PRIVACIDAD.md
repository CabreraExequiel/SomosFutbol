# Legal y privacidad — checklist previo a producción

El proyecto incorpora una base técnica para consentimiento, reglas de uso y privacidad, pero un texto genérico no puede eliminar toda responsabilidad del operador. Las normas imperativas, los derechos de consumidores y las obligaciones propias de la plataforma siguen siendo aplicables. Antes de abrir el servicio al público, un profesional matriculado debe revisar los documentos según el modelo comercial, la jurisdicción y la operatoria real de pagos.

## Implementado

- aceptación separada y obligatoria de Términos y Política de Privacidad;
- registro en servidor de fecha y versión de ambas aceptaciones;
- prohibición de registro público como administrador;
- teléfono obligatorio y validado en servidor;
- reglas sobre uso ilícito, fraude, acoso, suplantación, contenido y seguridad;
- facultades de moderación, suspensión, preservación de evidencia y colaboración con autoridades;
- obligaciones específicas para dueños de canchas;
- reconocimiento expreso de que no se excluyen responsabilidades legales ni derechos irrenunciables;
- información sobre datos tratados, finalidades, destinatarios, conservación, seguridad y derechos;
- datos de cobro separados del perfil público y respuestas privadas con `Cache-Control: no-store`;
- acceso a la cuenta del dueño limitado al propio dueño o al jugador vinculado con una reserva manual.

## Datos que el titular debe completar

La identidad del operador se carga desde `LEGAL_OPERATOR`, `LEGAL_TAX_ID`, `LEGAL_ADDRESS` y `SUPPORT_EMAIL` y se muestra en las páginas legales. Antes del lanzamiento completá y revisá:

1. nombre completo o razón social;
2. CUIT;
3. domicilio legal;
4. correo específico para soporte, reportes y ejercicio de derechos;
5. jurisdicción y autoridad local que correspondan al domicilio y actividad;
6. reglas reales de cancelación, devolución, comisiones y facturación;
7. política de retención con plazos concretos;
8. procedimiento interno de denuncias, bloqueo, preservación y respuesta a incidentes.

## Versionado

Las variables `TERMS_VERSION` y `PRIVACY_VERSION` se guardan en cada registro. No cambies solamente el texto visible: cuando haya una modificación material, actualizá la versión y agregá un mecanismo que bloquee o limite el uso hasta que las cuentas existentes vuelvan a aceptar. No marques automáticamente como aceptado un documento que el usuario no vio.

La migración mantiene vacíos los consentimientos de usuarios anteriores porque inventar una aceptación histórica sería incorrecto. Planificá una campaña de reaceptación al desplegar esta versión.

## Pagos y transferencias

El alias o CBU/CVU habilita una transferencia manual. El sistema no puede verificar por sí solo que la transferencia fue realizada: el dueño debe comprobarla y confirmar la reserva. No aceptes como prueba únicamente una captura de pantalla.

Checkout Pro utiliza la cuenta que cada dueño vincula mediante OAuth. Los tokens se cifran y se conservan en el servidor. La app comparte nombre, foto, ciudad y datos de contacto del jugador con el dueño de su reserva para coordinarla; no entrega contraseñas ni información bancaria del jugador. La integración no incluye comisiones propias ni devoluciones automáticas. El operador debe definir el procedimiento de conciliación, reembolsos y facturación para su actividad.

## Fuentes oficiales de referencia

- Ley 25.326 de Protección de los Datos Personales: <https://www.argentina.gob.ar/normativa/nacional/64790/actualizacion>
- Derechos de titulares y consentimiento según la AAIP: <https://www.argentina.gob.ar/aaip/datospersonales/derechos>
- Ley 24.240 de Defensa del Consumidor: <https://servicios.infoleg.gob.ar/infolegInternet/verNorma.do?id=638>
- Alias CVU según el BCRA: <https://www.bcra.gob.ar/alias-cvu/>

Este archivo es una guía de implementación y no reemplaza asesoramiento jurídico, contable ni tributario.
