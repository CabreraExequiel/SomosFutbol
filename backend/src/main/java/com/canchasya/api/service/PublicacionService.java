
package com.canchasya.api.service;

import com.canchasya.api.dto.*;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final ComentarioRepository comentarioRepository;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final AmistadService amistadService;
    private final AmistadRepository amistadRepository;
    private final NotificacionService notificacionService;
    private final VideoStorageService videoStorageService;

    private static final int MAX_CANDIDATOS = 500; // Se mantiene para el filtro Cerca.
    private static final int MAX_PAGINA = 50;
    private static final int TAMANO_LOTE_RANKING = 200;
    // Limitar cuantos resultados hay que retener para evitar consumos excesivos
    // si alguien pide una pagina extremadamente lejana.
    private static final int MAX_RESULTADOS_RANKING = 5000;

    private record PublicacionPuntuada(Long id, LocalDateTime fecha, double puntaje) {}

    private static final Comparator<PublicacionPuntuada> MEJOR_PRIMERO = Comparator
            .comparingDouble(PublicacionPuntuada::puntaje).reversed()
            .thenComparing(PublicacionPuntuada::fecha,
                    Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PublicacionPuntuada::id, Comparator.reverseOrder());

    private static final Set<String> MODOS = Set.of(
            "para-vos",
            "recientes",
            "amigos",
            "cerca"
    );

    // ==========================================
    // FEED PERSONALIZADO
    // ==========================================

    @Transactional(readOnly = true)
    public List<PublicacionResponse> feed(
            User user,
            int page,
            int size,
            String modo
    ) {

        if (user == null) {
            throw new ApiException(
                    "Debés iniciar sesión",
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (page < 0 || size < 1 || size > MAX_PAGINA) {
            throw new ApiException(
                    "Parámetros de paginación inválidos",
                    HttpStatus.BAD_REQUEST
            );
        }

        String vista = modo == null
                ? "para-vos"
                : modo.trim().toLowerCase(Locale.ROOT);

        if (!MODOS.contains(vista)) {
            throw new ApiException(
                    "Filtro del feed inválido",
                    HttpStatus.BAD_REQUEST
            );
        }

        Long usuarioId = user.getId();

        // Modo cronológico: la página se resuelve en MySQL.
        // No carga los primeros 500 candidatos ni calcula rankings en memoria.
        // Orden secundario por ID evita resultados inestables con fechas iguales.
        if (vista.equals("recientes")) {
            List<Publicacion> paginaRecientes = publicacionRepository
                    .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(page, size))
                    .getContent();
            return convertirConLikes(paginaRecientes, usuarioId);
        }

        // 05A4: Amigos muestra solo autores aceptados y publicaciones propias.
        // Filtrado + orden + paginación ocurren en MySQL, incluso más allá de 500.
        if (vista.equals("amigos")) {
            Set<Long> autores = idsAmigos(usuarioId);
            autores.add(usuarioId);
            List<Publicacion> paginaAmigos = publicacionRepository
                    .findFeedAmigos(autores, PageRequest.of(page, size))
                    .getContent();
            return convertirConLikes(paginaAmigos, usuarioId);
        }

        // 05A5: para-vos evalua TODAS las publicaciones con lotes ligeros
        // y conserva unicamente los mejores resultados necesarios.
        if (vista.equals("para-vos")) {
            return feedParaVos(user, page, size);
        }

        // El modo Cerca conserva por ahora el limite de candidatos original.

        List<Publicacion> candidatos =
                publicacionRepository
                        .findAllByOrderByCreatedAtDesc(
                                PageRequest.of(
                                        0,
                                        MAX_CANDIDATOS
                                )
                        )
                        .getContent();

        if (candidatos.isEmpty()) {
            return List.of();
        }

        String ciudadUsuario = normalizarCiudad(user.getCiudad());

        // ======================================
        // FILTRAR PUBLICACIONES
        // ======================================

        List<Publicacion> filtradas = candidatos
                .stream()
                .filter(publicacion -> {

                    return !ciudadUsuario.isEmpty()
                            && ciudadUsuario.equals(normalizarCiudad(
                                    publicacion.getUser().getCiudad()));
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // Cerca sigue ordenado cronologicamente, igual que antes.
        filtradas.sort(
                Comparator.comparing(Publicacion::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Publicacion::getId, Comparator.reverseOrder())
        );

        // ======================================
        // PAGINACIÓN
        // ======================================

        long inicio = (long) page * size;

        if (inicio >= filtradas.size()) {
            return List.of();
        }

        int desde = (int) inicio;
        int hasta = (int) Math.min(
                filtradas.size(),
                inicio + size
        );

        return convertirConLikes(filtradas.subList(desde, hasta), usuarioId);
    }

    // 05B1: busqueda local de etiquetas en publicaciones, sin tablas nuevas.
    // Se valida la etiqueta para evitar expresiones regulares arbitrarias
    // y se respetan las mismas reglas de tamaño del feed.
    @Transactional(readOnly = true)
    public List<PublicacionResponse> buscarPorHashtag(
            User usuario, String etiqueta, int page, int size) {
        if (usuario == null) {
            throw new ApiException("Debés iniciar sesión", HttpStatus.UNAUTHORIZED);
        }
        if (page < 0 || page > 500 || size < 1 || size > MAX_PAGINA) {
            throw new ApiException("Parámetros de paginación inválidos", HttpStatus.BAD_REQUEST);
        }

        String nombre = etiqueta == null ? "" : etiqueta.trim();
        if (nombre.startsWith("#")) {
            nombre = nombre.substring(1);
        }
        if (!nombre.matches("[A-Za-z0-9_]{2,30}")) {
            throw new ApiException(
                    "Usá una etiqueta de 2 a 30 caracteres (letras, números o _)",
                    HttpStatus.BAD_REQUEST);
        }

        // REGEXP compatible con MySQL 8 y MariaDB 10.4. El limite de
        // palabra incluye el # inicial y el final de la etiqueta.
        String patron = "(^|[^[:alnum:]_])#" + nombre + "([^[:alnum:]_]|$)";
        List<Publicacion> pagina = publicacionRepository.buscarPorHashtag(
                patron, PageRequest.of(page, size)).getContent();
        return convertirConLikes(pagina, usuario.getId());
    }

    // 05A5: ranking global con consultas pequenas. El DTO no es una entidad
    // gestionada por Hibernate: evita acumular miles de Publicacion en el
    // contexto de persistencia. El heap mantiene solo los mejores K resultados.
    private List<PublicacionResponse> feedParaVos(User usuario, int page, int size) {
        long desde = (long) page * size;
        if (desde >= MAX_RESULTADOS_RANKING) {
            return List.of();
        }
        int hasta = (int) Math.min(desde + size, MAX_RESULTADOS_RANKING);
        Long usuarioId = usuario.getId();
        Set<Long> amigos = idsAmigos(usuarioId);
        Set<Long> autoresPreferidos = new HashSet<>(likeRepository.autoresPreferidos(usuarioId));
        autoresPreferidos.addAll(comentarioRepository.autoresComentados(usuarioId));
        String ciudadUsuario = normalizarCiudad(usuario.getCiudad());
        LocalDateTime ahora = LocalDateTime.now();

        // PriorityQueue deja el PEOR resultado en la cabeza para reemplazarlo
        // cuando aparece una publicacion mas relevante en un lote posterior.
        PriorityQueue<PublicacionPuntuada> mejores =
                new PriorityQueue<>(hasta, MEJOR_PRIMERO.reversed());

        FeedCandidato cursor = null;
        PageRequest loteDeDoscientas = PageRequest.of(0, TAMANO_LOTE_RANKING);
        while (true) {
            Slice<FeedCandidato> lote = cursor == null
                    ? publicacionRepository.listarCandidatosParaVos(loteDeDoscientas)
                    : publicacionRepository.listarCandidatosParaVosAntesDe(
                            cursor.createdAt(), cursor.id(), loteDeDoscientas);
            List<FeedCandidato> candidatos = lote.getContent();
            if (candidatos.isEmpty()) {
                break;
            }

            Set<Long> ids = candidatos.stream().map(FeedCandidato::id)
                    .collect(Collectors.toSet());
            Map<Long, Long> likes = new HashMap<>();
            for (Object[] fila : likeRepository.contarPorPublicaciones(ids)) {
                likes.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
            }
            Map<Long, Long> comentarios = new HashMap<>();
            for (Object[] fila : comentarioRepository.contarPorPublicaciones(ids)) {
                comentarios.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
            }

            for (FeedCandidato candidato : candidatos) {
                double puntaje = calcularPuntaje(
                        candidato.autorId(), candidato.ciudadAutor(),
                        candidato.createdAt(), usuarioId, ciudadUsuario,
                        amigos, autoresPreferidos, ahora,
                        likes.getOrDefault(candidato.id(), 0L),
                        comentarios.getOrDefault(candidato.id(), 0L));
                PublicacionPuntuada actual = new PublicacionPuntuada(
                        candidato.id(), candidato.createdAt(), puntaje);
                if (mejores.size() < hasta) {
                    mejores.offer(actual);
                } else if (MEJOR_PRIMERO.compare(actual, mejores.peek()) < 0) {
                    mejores.poll();
                    mejores.offer(actual);
                }
            }

            if (!lote.hasNext()) {
                break;
            }
            cursor = candidatos.get(candidatos.size() - 1);
        }

        if (desde >= mejores.size()) {
            return List.of();
        }
        List<PublicacionPuntuada> ordenadas = new ArrayList<>(mejores);
        ordenadas.sort(MEJOR_PRIMERO);
        List<Long> idsPagina = ordenadas.subList((int) desde, ordenadas.size())
                .stream().map(PublicacionPuntuada::id).toList();

        // Recuperar las entidades completas SOLO de la pagina final, respetando
        // el orden del ranking y no el orden arbitrario del SELECT ... IN.
        Map<Long, Publicacion> porId = new HashMap<>();
        publicacionRepository.findAllById(idsPagina)
                .forEach(publicacion -> porId.put(publicacion.getId(), publicacion));
        List<Publicacion> pagina = idsPagina.stream()
                .map(porId::get).filter(Objects::nonNull).toList();
        return convertirConLikes(pagina, usuarioId);
    }

    // Reutilizar la misma definición de amistad aceptada en Amigos y Para vos.
    private Set<Long> idsAmigos(Long usuarioId) {
        Set<Long> amigos = new HashSet<>();
        for (Amistad amistad : amistadRepository.findRelaciones(
                usuarioId, EstadoAmistad.ACEPTADA)) {
            Long solicitante = amistad.getSolicitante().getId();
            Long destinatario = amistad.getDestinatario().getId();
            amigos.add(solicitante.equals(usuarioId) ? destinatario : solicitante);
        }
        return amigos;
    }

    private List<PublicacionResponse> convertirConLikes(
            List<Publicacion> publicacionesPagina,
            Long usuarioId
    ) {
        if (publicacionesPagina.isEmpty()) {
            return List.of();
        }

        Set<Long> idsPagina = publicacionesPagina.stream()
                .map(Publicacion::getId)
                .collect(Collectors.toSet());

        // 05A1: una consulta de likes para toda la página.
        Set<Long> idsLikeados = likeRepository.idsLikeadosPorUsuario(
                usuarioId, idsPagina
        );

        return publicacionesPagina.stream()
                .map(publicacion -> PublicacionResponse.fromEntity(
                        publicacion,
                        idsLikeados.contains(publicacion.getId())
                ))
                .toList();
    }

    // ==========================================
    // ALGORITMO DE RECOMENDACIONES
    // ==========================================

    private double calcularPuntaje(
            Long autorId,
            String ciudadAutor,
            LocalDateTime createdAt,
            Long usuarioId,
            String ciudadUsuario,
            Set<Long> amigos,
            Set<Long> autoresPreferidos,
            LocalDateTime ahora,
            long likes,
            long comentarios
    ) {

        double puntos = 0;

        // 1. AMISTADES
        if (amigos.contains(autorId)) {
            puntos += 35;
        }

        // 2. CERCANÍA GEOGRÁFICA
        ciudadAutor = normalizarCiudad(ciudadAutor);

        if (!ciudadUsuario.isEmpty()
                && ciudadUsuario.equals(ciudadAutor)) {
            puntos += 30;
        }

        // 3. INTERACCIONES HISTÓRICAS
        if (autoresPreferidos.contains(autorId)) {
            puntos += 25;
        }

        // 4. POPULARIDAD DE LA PUBLICACIÓN
        // Los conteos vienen de consultas agrupadas, no de colecciones LAZY.
        puntos += Math.min(
                20,
                Math.log1p(likes) * 7
        );

        puntos += Math.min(
                15,
                Math.log1p(comentarios) * 6
        );

        // 5. ANTIGÜEDAD
        if (createdAt != null) {

            long horas = Math.max(
                    0,
                    Duration.between(
                            createdAt,
                            ahora
                    ).toHours()
            );

            puntos += 35.0 / (1.0 + horas / 24.0);
        }

        // 6. PUBLICACIONES PROPIAS
        // Pequeño impulso para que el usuario
        // pueda encontrar su propio contenido.
        if (autorId.equals(usuarioId)) {
            puntos += 5;
        }

        return puntos;
    }

    // ==========================================
    // NORMALIZACIÓN DE CIUDADES
    // ==========================================

    private String normalizarCiudad(String ciudad) {

        if (ciudad == null || ciudad.isBlank()) {
            return "";
        }

        String resultado = Normalizer.normalize(
                ciudad.strip().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        return resultado
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ==========================================
    // CREAR PUBLICACIÓN
    // ==========================================

    @Transactional
    public PublicacionResponse crear(
            PublicacionRequest request,
            User user
    ) {

        Publicacion publicacion = new Publicacion();

        publicacion.setUser(user);
        publicacion.setContenido(request.getContenido());
        publicacion.setImagen(request.getImagen());
        publicacion.setYoutubeVideoId(YoutubeVideoUtil.extraerId(request.getYoutubeUrl()));

        Set<User> mencionados = resolverMenciones(
                request.getMencionadosIds(),
                user
        );

        publicacion.setMencionados(mencionados);

        publicacionRepository.save(publicacion);

        notificarMenciones(
                mencionados,
                user,
                "Te etiquetaron en una publicación",
                user.getName() + " te etiquetó en una publicación.",
                "/inicio",
                publicacion.getId()
        );

        return PublicacionResponse.fromEntity(
                publicacion,
                false
        );
    }



    // ==========================================
    // CREAR PUBLICACIÓN CON VIDEO
    // ==========================================

    @Transactional
    public PublicacionResponse crearConVideo(
            String contenido,
            MultipartFile video,
            Set<Long> mencionadosIds,
            User user
    ) {

        // Verificar autenticación
        if (user == null) {
            throw new ApiException(
                    "Debés iniciar sesión",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // Validar contenido
        if (contenido == null ||
                contenido.isBlank() ||
                contenido.length() > 2000) {

            throw new ApiException(
                    "La descripción debe tener entre 1 y 2000 caracteres",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Validar y resolver menciones
        Set<User> mencionados = resolverMenciones(
                mencionadosIds,
                user
        );

        // Guardar archivo de video
        VideoStorageService.VideoGuardado videoGuardado =
                videoStorageService.guardar(video);

        try {

            // Crear publicación
            Publicacion publicacion = new Publicacion();

            publicacion.setUser(user);
            publicacion.setContenido(contenido.trim());

            // Esta publicación contiene un video
            publicacion.setImagen(null);

            publicacion.setVideoUrl(
                    videoGuardado.url()
            );

            publicacion.setVideoPublicId(
                    videoGuardado.publicId()
            );

            // Las miniaturas se incorporarán después
            publicacion.setVideoThumbnailUrl(null);

            publicacion.setMencionados(mencionados);

            // Persistir en MariaDB
            publicacionRepository.saveAndFlush(publicacion);

            // Notificar usuarios mencionados
            notificarMenciones(
                    mencionados,
                    user,
                    "Te etiquetaron en una publicación",
                    user.getName()
                            + " te etiquetó en un video.",
                    "/inicio",
                    publicacion.getId()
            );

            // Devolver publicación creada
            return PublicacionResponse.fromEntity(
                    publicacion,
                    false
            );

        } catch (RuntimeException e) {

            // Evitar dejar archivos huérfanos
            // si falla la creación de la publicación.
            try {
                videoStorageService.eliminar(
                        videoGuardado.publicId()
                );
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }

            throw e;
        }
    }

    // ==========================================
    // ELIMINAR PUBLICACIÓN
    // ==========================================

    @Transactional
    public void eliminar(Long id, User user) {

        Publicacion publicacion = obtener(id);

        if (!publicacion.getUser().getId()
                .equals(user.getId())) {

            throw new ApiException(
                    "No podés eliminar esta publicación",
                    HttpStatus.FORBIDDEN
            );
        }

        publicacionRepository.delete(publicacion);
    }

    // ==========================================
    // COMENTARIOS
    // ==========================================

    @Transactional
    public ComentarioResponse comentar(
            Long publicacionId,
            ComentarioRequest request,
            User user
    ) {

        Publicacion publicacion = obtener(publicacionId);

        Comentario comentario = new Comentario();

        comentario.setPublicacion(publicacion);
        comentario.setUser(user);
        comentario.setContenido(request.getContenido());

        comentarioRepository.save(comentario);

        notificacionService.crear(
                publicacion.getUser(),
                user,
                TipoNotificacion.COMENTARIO,
                "Nuevo comentario",
                user.getName() + " comentó tu publicación.",
                "/inicio",
                publicacion.getId()
        );

        notificarMenciones(
                resolverMenciones(
                        request.getMencionadosIds(),
                        user
                ),
                user,
                "Te etiquetaron en un comentario",
                user.getName() + " te etiquetó en un comentario.",
                "/inicio",
                publicacion.getId()
        );

        return ComentarioResponse.fromEntity(comentario);
    }

    // ==========================================
    // ME GUSTA
    // ==========================================

    @Transactional
    public boolean toggleLike(
            Long publicacionId,
            User user
    ) {

        Publicacion publicacion = obtener(publicacionId);

        var existente =
                likeRepository.findByPublicacionIdAndUserId(
                        publicacionId,
                        user.getId()
                );

        if (existente.isPresent()) {

            likeRepository.delete(existente.get());

            return false;
        }

        Like like = new Like();

        like.setPublicacion(publicacion);
        like.setUser(user);

        likeRepository.save(like);

        notificacionService.crear(
                publicacion.getUser(),
                user,
                TipoNotificacion.ME_GUSTA,
                "A alguien le gustó tu publicación",
                user.getName()
                        + " indicó que le gusta tu publicación.",
                "/inicio",
                publicacion.getId()
        );

        return true;
    }

    // ==========================================
    // NOTIFICAR ETIQUETAS
    // ==========================================

    private void notificarMenciones(
            Set<User> mencionados,
            User actor,
            String titulo,
            String mensaje,
            String destino,
            Long referenciaId
    ) {

        if (mencionados == null || mencionados.isEmpty()) {
            return;
        }

        mencionados.forEach(destinatario ->
                notificacionService.crear(
                        destinatario,
                        actor,
                        TipoNotificacion.ETIQUETA,
                        titulo,
                        mensaje,
                        destino,
                        referenciaId
                )
        );
    }

    // ==========================================
    // RESOLVER ETIQUETAS DE AMIGOS
    // ==========================================

    private Set<User> resolverMenciones(
            Set<Long> mencionadosIds,
            User actor
    ) {

        if (mencionadosIds == null
                || mencionadosIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        return mencionadosIds
                .stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(actor.getId()))
                .distinct()
                .limit(20)
                .filter(id ->
                        amistadService.sonAmigos(
                                actor.getId(),
                                id
                        )
                )
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .collect(
                        Collectors.toCollection(
                                LinkedHashSet::new
                        )
                );
    }

    // ==========================================
    // COMPROBAR LIKE DEL USUARIO
    // ==========================================

    private boolean yaLeDioLike(
            Long publicacionId,
            Long usuarioId
    ) {

        return likeRepository
                .findByPublicacionIdAndUserId(
                        publicacionId,
                        usuarioId
                )
                .isPresent();
    }

    // ==========================================
    // OBTENER PUBLICACIÓN
    // ==========================================

    private Publicacion obtener(Long id) {

        return publicacionRepository
                .findById(id)
                .orElseThrow(() ->
                        new ApiException(
                                "Publicación no encontrada",
                                HttpStatus.NOT_FOUND
                        )
                );
    }
}
