package com.canchasya.api.controller;

import com.canchasya.api.dto.AmistadResponse;
import com.canchasya.api.dto.UsuarioSocialResponse;
import com.canchasya.api.model.User;
import com.canchasya.api.service.AmistadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amistades")
@RequiredArgsConstructor
public class AmistadController {

    private final AmistadService amistadService;

    @GetMapping
    public ResponseEntity<List<AmistadResponse>> listar(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(amistadService.listarAmigos(user));
    }

    @GetMapping("/solicitudes/recibidas")
    public ResponseEntity<List<AmistadResponse>> recibidas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(amistadService.solicitudesRecibidas(user));
    }

    @GetMapping("/solicitudes/enviadas")
    public ResponseEntity<List<AmistadResponse>> enviadas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(amistadService.solicitudesEnviadas(user));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioSocialResponse>> buscar(
            @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(amistadService.buscarUsuarios(user, q));
    }

    @PostMapping("/solicitudes/{userId}")
    public ResponseEntity<AmistadResponse> enviar(
            @PathVariable Long userId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(amistadService.enviarSolicitud(userId, user));
    }

    @PatchMapping("/solicitudes/{id}/aceptar")
    public ResponseEntity<AmistadResponse> aceptar(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(amistadService.aceptar(id, user));
    }

    @PatchMapping("/solicitudes/{id}/rechazar")
    public ResponseEntity<Void> rechazar(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        amistadService.rechazar(id, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{amigoId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long amigoId,
            @AuthenticationPrincipal User user
    ) {
        amistadService.eliminarAmigo(amigoId, user);
        return ResponseEntity.noContent().build();
    }
}
