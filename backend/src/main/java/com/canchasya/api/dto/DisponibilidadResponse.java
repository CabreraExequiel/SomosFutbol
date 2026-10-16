package com.canchasya.api.dto;
import java.time.LocalDate;
import java.util.List;
public record DisponibilidadResponse(Long canchaId, LocalDate fecha, String zonaHoraria,
    List<FranjaResponse> publicados, List<FranjaResponse> libres, List<FranjaResponse> ocupados) {}
