package com.canchasya.api.controller;

import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.model.User;
import com.canchasya.api.service.PublicidadAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** 10A: endpoints administrativos. Seguridad también reforzada por SecurityConfig. */
@RestController
@RequestMapping("/api/admin/publicidades")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class PublicidadAdminController {
    private final PublicidadAdminService service;

    @GetMapping
    public ResponseEntity<Page<PublicidadAdminService.Respuesta>> listar(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(required = false) EstadoPublicidad estado,
            @RequestParam(required = false) UbicacionPublicidad ubicacion,
            @RequestParam(defaultValue = "0") int pagina) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.listar(buscar, estado, ubicacion, pagina));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicidadAdminService.Respuesta> detalle(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PublicidadAdminService.Respuesta> crear(
            @RequestBody PublicidadAdminService.Solicitud solicitud, Authentication auth) {
        var resultado = service.crear(solicitud, administrador(auth));
        return ResponseEntity.created(URI.create("/api/admin/publicidades/" + resultado.id()))
                .cacheControl(CacheControl.noStore()).body(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicidadAdminService.Respuesta> editar(
            @PathVariable Long id, @RequestBody PublicidadAdminService.Solicitud solicitud,
            Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.editar(id, solicitud, administrador(auth)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PublicidadAdminService.Respuesta> estado(
            @PathVariable Long id, @RequestBody PublicidadAdminService.CambioEstado solicitud,
            Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.cambiarEstado(id, solicitud, administrador(auth)));
    }

    private static Long administrador(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }
}
