package com.canchasya.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class PublicacionRequest {

    @NotBlank
    @Size(max = 2000)
    private String contenido;

    @Size(max = 500)
    private String imagen;

    @Size(max = 500)
    private String youtubeUrl;

    private Set<Long> mencionadosIds = Set.of();
}
