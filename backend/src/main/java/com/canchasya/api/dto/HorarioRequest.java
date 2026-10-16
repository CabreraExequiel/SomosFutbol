package com.canchasya.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class HorarioRequest {

    @NotNull
    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(6)
    private Integer diaSemana;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;
}
