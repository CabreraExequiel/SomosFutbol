package com.canchasya.api.dto.auth;

import com.canchasya.api.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** En login sólo se necesita credential. En el primer registro se piden los demás datos. */
public record GoogleAuthRequest(
        @NotBlank String credential,
        @Size(max = 30) String telefono,
        @Size(max = 100) String ciudad,
        Role role,
        Boolean aceptaTerminos,
        Boolean aceptaPrivacidad
) {}
