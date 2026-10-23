package com.canchasya.api.controller;

import com.canchasya.api.dto.auth.AuthResponse;
import com.canchasya.api.dto.auth.LoginRequest;
import com.canchasya.api.dto.auth.RegisterRequest;
import com.canchasya.api.dto.auth.GoogleAuthRequest;
import com.canchasya.api.service.AuthService;
import com.canchasya.api.service.GoogleAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.autenticar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }
}
