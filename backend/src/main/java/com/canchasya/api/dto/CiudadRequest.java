package com.canchasya.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CiudadRequest {
    @NotBlank
    private String nombre;
    private String provincia;
}
