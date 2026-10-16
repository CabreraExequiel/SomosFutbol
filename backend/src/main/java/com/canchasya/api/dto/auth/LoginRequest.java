package com.canchasya.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    @Email
    @jakarta.validation.constraints.Size(max=150)
    private String email;

    @NotBlank
    @jakarta.validation.constraints.Size(max=72)
    private String password;
}
