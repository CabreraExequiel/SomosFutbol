package com.canchasya.api.controller;

import com.canchasya.api.dto.*;
import com.canchasya.api.dto.ReservaResponse;
import com.canchasya.api.dto.DatosTransferenciaResponse;
import com.canchasya.api.model.User;
import com.canchasya.api.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> misReservas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.misReservas(user));
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<DisponibilidadResponse> disponibilidad(
            @RequestParam Long canchaId,
            @RequestParam LocalDate fecha
    ) {
        return ResponseEntity.ok(reservaService.disponibilidad(canchaId, fecha));
    }

    @GetMapping("/recibidas")
    public ResponseEntity<List<ReservaResponse>> reservasDeMisCanchas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.reservasDeMisCanchas(user));
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.crear(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> ver(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.ver(id, user));
    }

    @GetMapping("/{id}/detalle")
    public ResponseEntity<ReservaDetalleResponse> detalle(@PathVariable Long id,@AuthenticationPrincipal User user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reservaService.detalle(id,user));
    }

    @GetMapping("/{id}/datos-transferencia")
    public ResponseEntity<DatosTransferenciaResponse> datosTransferencia(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(reservaService.datosTransferencia(id, user));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.cancelar(id, user));
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<ReservaResponse> confirmar(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.confirmar(id, user));
    }
}
