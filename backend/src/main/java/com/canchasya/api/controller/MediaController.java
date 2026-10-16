package com.canchasya.api.controller;

import com.canchasya.api.service.ArchivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final ArchivoService archivoService;

    @GetMapping("/{carpeta}/{nombre:.+}")
    public ResponseEntity<Resource> ver(
            @PathVariable String carpeta,
            @PathVariable String nombre
    ) {
        Resource resource = archivoService.cargar(carpeta, nombre);
        MediaType tipo = nombre.toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(tipo)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .body(resource);
    }
}
