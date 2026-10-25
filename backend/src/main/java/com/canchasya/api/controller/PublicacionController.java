
package com.canchasya.api.controller;

import com.canchasya.api.dto.*;
import com.canchasya.api.model.User;
import com.canchasya.api.service.PublicacionService;
import com.canchasya.api.service.VideoStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/publicaciones")
@RequiredArgsConstructor
public class PublicacionController {

    private final PublicacionService publicacionService;
    private final VideoStorageService videoStorageService;

    private static final Set<String> MODOS_PERMITIDOS = Set.of(
            "para-vos",
            "recientes",
            "amigos",
            "cerca"
    );

    // ==========================================
    // FEED PERSONALIZADO
    // ==========================================

    @GetMapping
    public ResponseEntity<List<PublicacionResponse>> feed(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "para-vos") String modo
    ) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La página no puede ser negativa"
            );
        }

        if (size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tamaño de página debe estar entre 1 y 50"
            );
        }

        if (!MODOS_PERMITIDOS.contains(modo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Modo de feed no válido"
            );
        }

        return ResponseEntity.ok(
                publicacionService.feed(user, page, size, modo)
        );
    }

    // ==========================================
    // 05B1: BUSCAR PUBLICACIONES POR HASHTAG
    // ==========================================

    @GetMapping("/hashtags")
    public ResponseEntity<List<PublicacionResponse>> buscarHashtag(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "") String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                publicacionService.buscarPorHashtag(user, tag, page, size));
    }

    // ==========================================
    // CREAR PUBLICACIÓN NORMAL
    // ==========================================

    @PostMapping
    public ResponseEntity<PublicacionResponse> crear(
            @Valid @RequestBody PublicacionRequest request,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                publicacionService.crear(request, user)
        );
    }

    // ==========================================
    // NUEVO: CREAR PUBLICACIÓN CON VIDEO
    // ==========================================

    @PostMapping(
            value = "/videos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PublicacionResponse> crearConVideo(
            @RequestParam("contenido") String contenido,
            @RequestParam("video") MultipartFile video,
            @RequestParam(
                    value = "mencionadosIds",
                    required = false
            ) Set<Long> mencionadosIds,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                publicacionService.crearConVideo(
                        contenido,
                        video,
                        mencionadosIds == null
                                ? Set.of()
                                : mencionadosIds,
                        user
                )
        );
    }


    // ==========================================
    // REPRODUCIR VIDEO
    // ==========================================

    @GetMapping("/videos/archivo/{publicId}")
    public ResponseEntity<Resource> reproducirVideo(
            @PathVariable String publicId
    ) {

        Resource video = videoStorageService.obtener(publicId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(video);
    }


    // ==========================================
    // ELIMINAR PUBLICACIÓN
    // ==========================================



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {

        publicacionService.eliminar(id, user);

        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // COMENTAR PUBLICACIÓN
    // ==========================================

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(
            @PathVariable Long id,
            @Valid @RequestBody ComentarioRequest request,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                publicacionService.comentar(id, request, user)
        );
    }

    // ==========================================
    // ME GUSTA
    // ==========================================

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {

        boolean likeado =
                publicacionService.toggleLike(id, user);

        return ResponseEntity.ok(
                Map.of("likeado", likeado)
        );
    }
}
