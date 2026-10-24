
package com.canchasya.api.service;

import com.canchasya.api.dto.*;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
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

    private static final int MAX_CANDIDATOS = 500;
    private static final int MAX_PAGINA = 50;

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

        // Otros modos conservan el ranking y filtros originales por ahora.
        // La migración del modo "para-vos" requiere preservar su orden de afinidad.

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

        // ======================================
        // AMISTADES CONFIRMADAS
        // ======================================

        Set<Long> amigos = idsAmigos(usuarioId);

        // ======================================
        // AUTORES CON INTERACCIONES PREVIAS
        // ======================================

        Set<Long> autoresPreferidos = new HashSet<>();

        if (vista.equals("para-vos")) {
            autoresPreferidos.addAll(
                    likeRepository.autoresPreferidos(usuarioId)
            );

            autoresPreferidos.addAll(
                    comentarioRepository.autoresComentados(
                            usuarioId
                    )
            );
        }

        String ciudadUsuario =
                normalizarCiudad(user.getCiudad());

        LocalDateTime ahora = LocalDateTime.now();

        // ======================================
        // FILTRAR PUBLICACIONES
        // ======================================

        List<Publicacion> filtradas = candidatos
                .stream()
                .filter(publicacion -> {

                    Long autorId =
                            publicacion.getUser().getId();

                    return switch (vista) {

                        case "amigos" ->
                                amigos.contains(autorId)
                                        || autorId.equals(usuarioId);

                        case "cerca" ->
                                !ciudadUsuario.isEmpty()
                                        && ciudadUsuario.equals(
                                        normalizarCiudad(
                                                publicacion.getUser()
                                                        .getCiudad()
                                        )
                                );

                        default -> true;
                    };
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // ======================================
        // ORDENAR EL FEED
        // ======================================

        if (vista.equals("para-vos")) {

            // 05A3: dos agregaciones para todas las candidatas, en lugar de
            // inicializar las colecciones LAZY de likes y comentarios de cada una.
            Set<Long> idsCandidatos = filtradas.stream()
                    .map(Publicacion::getId)
                    .collect(Collectors.toSet());

            Map<Long, Long> conteoLikes = new HashMap<>();
            for (Object[] fila : likeRepository.contarPorPublicaciones(idsCandidatos)) {
                conteoLikes.put(((Number) fila[0]).longValue(),
                        ((Number) fila[1]).longValue());
            }

            Map<Long, Long> conteoComentarios = new HashMap<>();
            for (Object[] fila : comentarioRepository.contarPorPublicaciones(idsCandidatos)) {
                conteoComentarios.put(((Number) fila[0]).longValue(),
                        ((Number) fila[1]).longValue());
            }

            Map<Long, Double> puntajes = new HashMap<>();

            for (Publicacion publicacion : filtradas) {

                puntajes.put(
                        publicacion.getId(),
                        calcularPuntaje(
                                publicacion,
                                usuarioId,
                                ciudadUsuario,
                                amigos,
                                autoresPreferidos,
                                ahora,
                                conteoLikes.getOrDefault(publicacion.getId(), 0L),
                                conteoComentarios.getOrDefault(publicacion.getId(), 0L)
                        )
                );
            }

            filtradas.sort(
                    Comparator
                            .comparingDouble(
                                    (Publicacion p) ->
                                            puntajes.get(p.getId())
                            )
                            .reversed()
                            .thenComparing(
                                    Publicacion::getCreatedAt,
                                    Comparator.nullsLast(
                                            Comparator.reverseOrder()
                                    )
                            )
                            .thenComparing(
                                    Publicacion::getId,
                                    Comparator.reverseOrder()
                            )
            );

        } else {

            filtradas.sort(
                    Comparator
                            .comparing(
                                    Publicacion::getCreatedAt,
                                    Comparator.nullsLast(
                                            Comparator.reverseOrder()
                                    )
                            )
                            .thenComparing(
                                    Publicacion::getId,
                                    Comparator.reverseOrder()
                            )
            );
        }

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
            Publicacion publicacion,
            Long usuarioId,
            String ciudadUsuario,
            Set<Long> amigos,
            Set<Long> autoresPreferidos,
            LocalDateTime ahora,
            long likes,
            long comentarios
    ) {

        Long autorId = publicacion.getUser().getId();

        double puntos = 0;

        // 1. AMISTADES
        if (amigos.contains(autorId)) {
            puntos += 35;
        }

        // 2. CERCANÍA GEOGRÁFICA
        String ciudadAutor = normalizarCiudad(
                publicacion.getUser().getCiudad()
        );

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
        if (publicacion.getCreatedAt() != null) {

            long horas = Math.max(
                    0,
                    Duration.between(
                            publicacion.getCreatedAt(),
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
