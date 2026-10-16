package com.canchasya.api.controller;

import com.canchasya.api.dto.NotificacionResponse;
import com.canchasya.api.model.User;
import com.canchasya.api.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificacionService.listar(user, limit));
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", notificacionService.contarNoLeidas(user)));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id, user));
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodas(@AuthenticationPrincipal User user) {
        notificacionService.marcarTodas(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal User user) {
        notificacionService.eliminar(id, user);
        return ResponseEntity.noContent().build();
    }
}
