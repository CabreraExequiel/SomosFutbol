package com.canchasya.api.controller;

import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.service.PublicidadPublicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Etapa 10C: consulta pública SOLO de anuncios que cumplen estado y fechas. */
@RestController
@RequestMapping("/api/publicidades")
@RequiredArgsConstructor
public class PublicidadPublicaController {
    private final PublicidadPublicaService service;

    @GetMapping("/visibles")
    public ResponseEntity<List<PublicidadPublicaService.Anuncio>> visibles(
            @RequestParam UbicacionPublicidad ubicacion) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.visibles(ubicacion));
    }
}
