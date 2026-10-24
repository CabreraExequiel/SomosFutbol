package com.canchasya.api.service;

import com.canchasya.api.dto.auth.AuthResponse;
import com.canchasya.api.dto.auth.GoogleAuthRequest;
import com.canchasya.api.dto.auth.GoogleLinkRequest;
import com.canchasya.api.dto.auth.UserResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.UserRepository;
import com.canchasya.api.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class GoogleAuthService {
    private final UserRepository users;
    private final JwtService jwt;
    private final PasswordEncoder encoder;
    private final GoogleIdTokenVerifier verifier;
    private final boolean configurado;
    private final String termsVersion;
    private final String privacyVersion;

    public GoogleAuthService(
            UserRepository users,
            JwtService jwt,
            PasswordEncoder encoder,
            @Value("${GOOGLE_CLIENT_ID:}") String clientId,
            @Value("${app.legal.terms-version}") String termsVersion,
            @Value("${app.legal.privacy-version}") String privacyVersion
    ) {
        this.users = users;
        this.jwt = jwt;
        this.encoder = encoder;
        this.termsVersion = termsVersion;
        this.privacyVersion = privacyVersion;
        this.configurado = clientId != null && !clientId.isBlank();
        this.verifier = configurado
                ? new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                        .setAudience(List.of(clientId.trim())).build()
                : null;
    }

    @Transactional
    public AuthResponse autenticar(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = verificarCredencial(request.credential());
        String sub = payload.getSubject();
        String email = payload.getEmail();
        if (!Boolean.TRUE.equals(payload.getEmailVerified()) || sub == null || sub.isBlank()
                || sub.length() > 255 || email == null || email.isBlank() || email.length() > 150) {
            throw new ApiException("La identidad de Google no tiene un correo verificado válido", HttpStatus.UNAUTHORIZED);
        }
        email = email.trim().toLowerCase(Locale.ROOT);

        // Una cuenta de Google siempre se identifica por sub, nunca por coincidencia de email.
        var existenteGoogle = users.findByGoogleSub(sub);
        if (existenteGoogle.isPresent()) {
            return sesion(existenteGoogle.get());
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(
                    "Ya existe una cuenta con ese correo. Iniciá sesión con tu contraseña; " +
                    "la vinculación con Google deberá hacerse desde tu perfil.",
                    HttpStatus.CONFLICT
            );
        }

        if (request.telefono() == null || !request.telefono().trim().matches("[0-9+()\\- ]{7,30}")) {
            throw new ApiException("Para registrarte con Google completá tu teléfono válido en Crear cuenta", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!Boolean.TRUE.equals(request.aceptaTerminos()) || !Boolean.TRUE.equals(request.aceptaPrivacidad())) {
            throw new ApiException("Para registrarte con Google aceptá Términos y Privacidad en Crear cuenta", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Role role = request.role() == null ? Role.USUARIO : request.role();
        if (role == Role.ADMINISTRADOR) {
            throw new ApiException("No se permite registrar administradores", HttpStatus.FORBIDDEN);
        }
        User user = new User();
        Object nombreGoogle = payload.get("name");
        String nombre = nombreGoogle instanceof String n && !n.isBlank() ? n.trim() : email.split("@")[0];
        user.setName(nombre.substring(0, Math.min(nombre.length(), 100)));
        user.setEmail(email);
        user.setGoogleSub(sub);
        user.setTelefono(request.telefono().trim());
        user.setCiudad(request.ciudad() == null || request.ciudad().isBlank() ? null : request.ciudad().trim());
        user.setRole(role);
        // Mantener password NOT NULL y hacer imposible el acceso por contraseña tradicional.
        user.setPassword(encoder.encode(UUID.randomUUID().toString()));        Instant now = Instant.now();
        user.setTermsAcceptedAt(now);
        user.setTermsVersion(termsVersion);
        user.setPrivacyAcceptedAt(now);
        user.setPrivacyVersion(privacyVersion);
        users.saveAndFlush(user);
        return sesion(user);
    }

    /**
     * Vincula Google a la cuenta que ya demostró estar autenticada con un JWT.
     * Requiere también la contraseña actual para impedir cambios de proveedor desde sesiones robadas.
     * Nunca selecciona una cuenta de Somos Fútbol basándose solo en el email de Google.
     */
    @Transactional
    public UserResponse vincular(GoogleLinkRequest request, User autenticado) {
        if (autenticado == null) {
            throw new ApiException("Necesitás iniciar sesión", HttpStatus.UNAUTHORIZED);
        }
        User actual = users.findByIdForUpdate(autenticado.getId())
                .orElseThrow(() -> new ApiException("Cuenta no encontrada", HttpStatus.UNAUTHORIZED));
        if (!encoder.matches(request.passwordActual(), actual.getPassword())) {
            throw new ApiException("La contraseña actual es incorrecta", HttpStatus.UNAUTHORIZED);
        }

        GoogleIdToken.Payload payload = verificarCredencial(request.credential());
        String sub = payload.getSubject();
        String emailGoogle = payload.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!actual.getEmail().trim().equalsIgnoreCase(emailGoogle)) {
            throw new ApiException("Elegí la cuenta de Google que utiliza el mismo correo que tu perfil", HttpStatus.CONFLICT);
        }
        if (actual.getGoogleSub() != null && !actual.getGoogleSub().isBlank()) {
            if (actual.getGoogleSub().equals(sub)) return UserResponse.fromEntity(actual);
            throw new ApiException("Esta cuenta ya tiene otra identidad de Google vinculada", HttpStatus.CONFLICT);
        }
        if (users.findByGoogleSub(sub).isPresent()) {
            throw new ApiException("Esta cuenta de Google ya está vinculada a otro usuario", HttpStatus.CONFLICT);
        }
        actual.setGoogleSub(sub);
        try {
            users.saveAndFlush(actual); // El índice UNIQUE protege contra una vinculación simultánea.
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException("Esta identidad de Google ya está vinculada a otra cuenta", HttpStatus.CONFLICT);
        }
        return UserResponse.fromEntity(actual);
    }

    private GoogleIdToken.Payload verificarCredencial(String credential) {
        if (!configurado) {
            throw new ApiException("Google no está configurado en este entorno", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (credential == null || credential.isBlank() || credential.length() > 15000) {
            throw new ApiException("Credencial de Google inválida", HttpStatus.UNAUTHORIZED);
        }
        GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (GeneralSecurityException ex) {
            throw new ApiException("Credencial de Google inválida", HttpStatus.UNAUTHORIZED);
        } catch (IOException ex) {
            throw new ApiException("No pudimos validar Google. Intentá nuevamente", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (token == null || token.getPayload() == null) {
            throw new ApiException("Credencial de Google inválida o vencida", HttpStatus.UNAUTHORIZED);
        }
        GoogleIdToken.Payload payload = token.getPayload();
        String sub = payload.getSubject();
        String email = payload.getEmail();
        if (!Boolean.TRUE.equals(payload.getEmailVerified()) || sub == null || sub.isBlank()
                || sub.length() > 255 || email == null || email.isBlank() || email.length() > 150) {
            throw new ApiException("La identidad de Google no tiene un correo verificado válido", HttpStatus.UNAUTHORIZED);
        }
        return payload;
    }

    private AuthResponse sesion(User user) {
        return new AuthResponse(jwt.generarToken(user), UserResponse.fromEntity(user));
    }
}
