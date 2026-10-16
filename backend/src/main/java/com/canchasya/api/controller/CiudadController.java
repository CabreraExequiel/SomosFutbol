package com.canchasya.api.controller;

import com.canchasya.api.dto.CiudadRequest;
import com.canchasya.api.dto.CiudadResponse;
import com.canchasya.api.service.CiudadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ciudades")
@RequiredArgsConstructor
public class CiudadController {

    private final CiudadService ciudadService;

    @GetMapping
    public ResponseEntity<List<CiudadResponse>> listar() {
        return ResponseEntity.ok(ciudadService.listar());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CiudadResponse> crear(@Valid @RequestBody CiudadRequest request) {
        return ResponseEntity.ok(ciudadService.crear(request));
    }
}
