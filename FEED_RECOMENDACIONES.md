# Somos Fútbol: feed de recomendaciones (beta)

## Cambios incluidos

- `GET /api/publicaciones?page=0&size=10&modo=para-vos` (predeterminado): ranking personalizado con ciudad del autor, amistades aceptadas, autores con los que interactuaste (likes/comentarios), popularidad y frescura.
- `modo=recientes`: orden cronológico.
- `modo=amigos`: publicaciones de amistades aceptadas y propias.
- `modo=cerca`: publicaciones de autores de tu misma ciudad (requiere ciudad en el perfil del usuario).
- Pestañas responsive en Angular, sin perder publicación, comentarios, etiquetas, likes y eliminación.
- No agrega ni elimina tablas; funciona con las entidades existentes. No modifica pagos ni reservas.

## Consideraciones

- Implementación de **beta**: rankea como máximo las 500 publicaciones más recientes en memoria por solicitud. En una red con miles de publicaciones, sustituir por una estrategia de candidatos e índices de base de datos y paginación por cursor; el algoritmo actual no garantiza mostrar publicaciones anteriores al límite.
- La ciudad se toma del perfil del **autor**, no necesariamente de una cancha asociada.
- El ranking usa reglas explícitas, no aprendizaje automático ni el algoritmo privado de Facebook.
- No se registran vistas o clics en esta versión; se aprovechan likes y comentarios existentes.
- El ranking puede cambiar entre páginas si se crean publicaciones o llegan interacciones mientras se navega; para escala real usar snapshots/cursor.
- Si la aplicación está ejecutándose contra MariaDB, seguir usando el driver MariaDB declarado en `pom.xml` y el dialecto correcto.

## Pruebas manuales sugeridas

1. Iniciá sesión con dos usuarios de ciudades distintas y creá publicaciones.
2. Comprobá que `Recientes` ordene por fecha y que `Cerca tuyo` muestre autores con la misma ciudad.
3. Creá una amistad aceptada y verificá `Amigos`.
4. Agregá likes/comentarios a publicaciones de un autor y verificá que `Para vos` priorice contenido relacionado.
5. Probá publicar, comentar, dar like, etiquetar y eliminar; comprobá también la paginación.

## Instalación

Reemplazá los archivos del proyecto con los de este ZIP, manteniendo tu configuración local `.env` fuera del repositorio. En el directorio de frontend, `npm ci` y `npm run build`; en backend, `mvn test` y `mvn package` (o sus comandos del wrapper, si está disponible). No fue posible ejecutar estas compilaciones dentro del entorno de edición: no dispone del entorno Maven ni de dependencias Node instaladas.
