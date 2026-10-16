
package com.canchasya.api.service;

import com.canchasya.api.dto.auth.AuthResponse;
import com.canchasya.api.dto.auth.LoginRequest;
import com.canchasya.api.dto.auth.RegisterRequest;
import com.canchasya.api.dto.auth.UserResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.User;
import com.canchasya.api.model.Role;
import com.canchasya.api.repository.UserRepository;
import com.canchasya.api.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.legal.terms-version}")
    private String termsVersion;

    @Value("${app.legal.privacy-version}")
    private String privacyVersion;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    "La contraseña es demasiado larga en bytes; usá hasta 72 bytes UTF-8",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(
                    "El email ya está registrado",
                    HttpStatus.CONFLICT
            );
        }

        Role role = request.getRole() == null
                ? Role.USUARIO
                : request.getRole();

        if (role == Role.ADMINISTRADOR) {
            throw new ApiException(
                    "No se puede crear una cuenta de administrador desde el registro público",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!request.isAceptaTerminos() || !request.isAceptaPrivacidad()) {
            throw new ApiException(
                    "Debés aceptar los Términos y la Política de Privacidad",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        User user = new User();

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTelefono(request.getTelefono().trim());
        user.setCiudad(limpiarOpcional(request.getCiudad()));
        user.setRole(role);

        // DATOS DE COBRO OPCIONALES
        // No se exige CBU/CVU ni alias durante el registro.
        // El dueño podrá configurarlos más adelante.
        user.setCbuCvu(null);
        user.setAliasMercadoPago(null);

        Instant aceptadoEn = Instant.now();

        user.setTermsAcceptedAt(aceptadoEn);
        user.setTermsVersion(termsVersion);

        user.setPrivacyAcceptedAt(aceptadoEn);
        user.setPrivacyVersion(privacyVersion);

        userRepository.save(user);

        // Envío de bienvenida desactivado
        // mailService.enviarBienvenida(user);

        String token = jwtService.generarToken(user);

        return new AuthResponse(
                token,
                UserResponse.fromEntity(user)
        );
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        "Credenciales inválidas",
                        HttpStatus.UNAUTHORIZED
                ));

        String token = jwtService.generarToken(user);

        return new AuthResponse(
                token,
                UserResponse.fromEntity(user)
        );
    }

    public UserResponse me(User user) {
        return UserResponse.fromEntity(user);
    }

    private String limpiarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
