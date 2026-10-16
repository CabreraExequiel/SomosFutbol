package com.canchasya.api.dto;

import com.canchasya.api.model.HorarioDisponible;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class HorarioResponse {

    private Long id;
    private Integer diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    public static HorarioResponse fromEntity(HorarioDisponible h) {
        return new HorarioResponse(h.getId(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin());
    }
}
