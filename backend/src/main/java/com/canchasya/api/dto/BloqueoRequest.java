package com.canchasya.api.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
public record BloqueoRequest(@NotNull LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
    @Size(max=200) String motivo) {}
