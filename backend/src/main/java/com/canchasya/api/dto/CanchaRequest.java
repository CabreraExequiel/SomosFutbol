package com.canchasya.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CanchaRequest {

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @Size(max = 255)
    private String direccion;

    @NotNull
    private Long ciudadId;

    @Size(max = 80)
    private String tipo;

    @Size(max = 80)
    private String superficie;

    @Min(2)
    @Max(30)
    private Integer cantJugadores;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioHora;

    private boolean techada;

    @Size(max = 500)
    private String imagen;

    @Size(max = 2000)
    private String descripcion;
}
