package com.canchasya.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
    private String name;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 7, max = 30, message = "El teléfono debe tener entre 7 y 30 caracteres")
    @Pattern(regexp = "^[0-9+()\\-\\s]+$", message = "El teléfono contiene caracteres inválidos")
    private String telefono;

    @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
    private String ciudad;

}
