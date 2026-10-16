package com.canchasya.api.controller;

import com.canchasya.api.dto.*;
import com.canchasya.api.service.DisponibilidadService;
import com.canchasya.api.dto.CanchaResponse;
import com.canchasya.api.dto.HorarioRequest;
import com.canchasya.api.dto.HorarioResponse;
import com.canchasya.api.model.User;
import com.canchasya.api.service.CanchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/canchas")
@RequiredArgsConstructor
public class CanchaController {

    private final CanchaService canchaService;
    private final DisponibilidadService disponibilidadService;

    @GetMapping("/duenos")
    public List<DuenoPublicoResponse> duenos(@RequestParam(required=false) Long ciudadId,@RequestParam(required=false) String tipo) {
        return canchaService.duenos(ciudadId,tipo);
    }
    @GetMapping("/duenos/{id}")
    public List<CanchaResponse> porDueno(@PathVariable Long id) {return canchaService.porDueno(id);}
    public record PublicacionRequest(@jakarta.validation.constraints.NotNull Boolean activa) {}
    @PatchMapping("/{id}/publicacion")
    public CanchaResponse publicar(@PathVariable Long id,@Valid @RequestBody PublicacionRequest request,@AuthenticationPrincipal User user) {
        return canchaService.publicar(id,request.activa(),user);
    }
    @PutMapping("/{id}/horarios")
    public List<HorarioResponse> agenda(@PathVariable Long id,@Valid @RequestBody AgendaRequest request,@AuthenticationPrincipal User user) {
        return disponibilidadService.reemplazar(id,request,user);
    }
    @DeleteMapping("/{id}/horarios/{horarioId}")
    public ResponseEntity<Void> quitarHorario(@PathVariable Long id,@PathVariable Long horarioId,@AuthenticationPrincipal User user) {
        canchaService.eliminarHorario(id,horarioId,user);return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/bloqueos")
    public List<BloqueoResponse> bloqueos(@PathVariable Long id,@AuthenticationPrincipal User user) {return disponibilidadService.bloqueos(id,user);}
    @PostMapping("/{id}/bloqueos")
    public BloqueoResponse bloquear(@PathVariable Long id,@Valid @RequestBody BloqueoRequest request,@AuthenticationPrincipal User user) {
        return disponibilidadService.bloquear(id,request,user);
    }
    @DeleteMapping("/{id}/bloqueos/{bloqueoId}")
    public ResponseEntity<Void> desbloquear(@PathVariable Long id,@PathVariable Long bloqueoId,@AuthenticationPrincipal User user) {
        disponibilidadService.desbloquear(id,bloqueoId,user);return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CanchaResponse>> buscar(
            @RequestParam(required = false) Long ciudadId,
            @RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(canchaService.buscar(ciudadId, tipo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CanchaResponse> ver(@PathVariable Long id) {
        return ResponseEntity.ok(canchaService.ver(id));
    }

    @GetMapping("/mias/todas")
    public ResponseEntity<List<CanchaResponse>> misCanchas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(canchaService.misCanchas(user));
    }

    @PostMapping
    public ResponseEntity<CanchaResponse> crear(@Valid @RequestBody CanchaRequest request,
                                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(canchaService.crear(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CanchaResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody CanchaRequest request,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(canchaService.actualizar(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal User user) {
        canchaService.eliminar(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CanchaResponse> actualizarImagen(
            @PathVariable Long id,
            @RequestPart("archivo") MultipartFile archivo,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(canchaService.actualizarImagen(id, archivo, user));
    }

    @GetMapping("/{id}/horarios")
    public ResponseEntity<List<HorarioResponse>> horarios(@PathVariable Long id,
                                                            @RequestParam(required = false) Integer diaSemana) {
        return ResponseEntity.ok(canchaService.horarios(id, diaSemana));
    }

    @PostMapping("/{id}/horarios")
    public ResponseEntity<HorarioResponse> agregarHorario(@PathVariable Long id,
                                                            @Valid @RequestBody HorarioRequest request,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(canchaService.agregarHorario(id, request, user));
    }
}
