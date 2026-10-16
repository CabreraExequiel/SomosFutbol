package com.canchasya.api.controller;

import com.canchasya.api.dto.UserUpdateRequest;
import com.canchasya.api.dto.CuentaCobroRequest;
import com.canchasya.api.dto.CuentaCobroResponse;
import com.canchasya.api.dto.auth.UserResponse;
import com.canchasya.api.model.User;
import com.canchasya.api.service.UserService;
import com.canchasya.api.service.CuentaCobroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CuentaCobroService cuentaCobroService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UserResponse>> listar() {
        return ResponseEntity.ok(userService.listar());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> perfil(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> actualizar(@Valid @RequestBody UserUpdateRequest request,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.actualizar(request, user));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> actualizarAvatar(
            @RequestPart("archivo") MultipartFile archivo,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(userService.actualizarAvatar(archivo, user));
    }

    @GetMapping("/me/cuenta-cobro")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<CuentaCobroResponse> obtenerCuentaCobro(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(cuentaCobroService.obtener(user));
    }

    @PutMapping("/me/cuenta-cobro")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<CuentaCobroResponse> actualizarCuentaCobro(
            @Valid @RequestBody CuentaCobroRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(cuentaCobroService.actualizar(request, user));
    }
}
