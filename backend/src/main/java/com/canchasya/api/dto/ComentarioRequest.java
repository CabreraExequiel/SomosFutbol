package com.canchasya.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class ComentarioRequest {

    @NotBlank
    @Size(max = 1000)
    private String contenido;

    private Set<Long> mencionadosIds = Set.of();
}
