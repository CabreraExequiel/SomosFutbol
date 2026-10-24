package com.canchasya.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** La contraseña actual se exige para vincular una identidad externa a una cuenta existente. */
public record GoogleLinkRequest(
        @NotBlank @Size(max = 15000) String credential,
        @NotBlank @Size(max = 72) String passwordActual
) {}
