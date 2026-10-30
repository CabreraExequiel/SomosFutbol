package com.canchasya.api.controller;

import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.service.PublicidadMetricasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PublicidadMetricasController {
    private final PublicidadMetricasService metricas;

    public record Registro(UbicacionPublicidad ubicacion) {}

    @PostMapping("/api/publicidades/{id}/impresiones")
    public ResponseEntity<Void> impresion(@PathVariable Long id, @RequestBody Registro registro) {
        metricas.registrar(id, registro == null ? null : registro.ubicacion(), false);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/api/publicidades/{id}/clics")
    public ResponseEntity<Void> clic(@PathVariable Long id, @RequestBody Registro registro) {
        metricas.registrar(id, registro == null ? null : registro.ubicacion(), true);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/api/admin/publicidades/estadisticas")
    public ResponseEntity<PublicidadMetricasService.Estadisticas> estadisticas(
            @RequestParam(defaultValue = "7") int dias,
            @RequestParam(required = false) UbicacionPublicidad ubicacion,
            @RequestParam(required = false) Long publicidadId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(metricas.estadisticas(dias, ubicacion, publicidadId));
    }
}
