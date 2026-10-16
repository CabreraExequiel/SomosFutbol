package com.canchasya.api.dto;
import com.canchasya.api.model.BloqueoCancha;
import java.time.LocalDate;
import java.time.LocalTime;
public record BloqueoResponse(Long id, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String motivo) {
    public static BloqueoResponse from(BloqueoCancha b) {
        return new BloqueoResponse(b.getId(),b.getFecha(),b.getHoraInicio(),b.getHoraFin(),b.getMotivo());
    }
}
