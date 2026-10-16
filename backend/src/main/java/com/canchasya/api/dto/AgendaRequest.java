package com.canchasya.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record AgendaRequest(@NotNull @Size(max=70) List<@NotNull @Valid HorarioRequest> horarios) {}
