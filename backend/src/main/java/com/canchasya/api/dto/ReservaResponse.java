package com.canchasya.api.dto;

import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.model.Reserva;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class ReservaResponse {

    private Long id;
    private Long userId;
    private String userNombre;
    private Long canchaId;
    private String canchaNombre;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal monto;
    private EstadoReserva estado;
    private EstadoPago estadoPago;
    private boolean pagoRequerido;
    private java.time.Instant expiresAt;

    public static ReservaResponse fromEntity(Reserva r) {
        return fromEntity(r, null, false);
    }

    public static ReservaResponse fromEntity(Reserva r, EstadoPago estadoPago, boolean pagoRequerido) {
        return new ReservaResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getName(),
                r.getCancha().getId(),
                r.getCancha().getNombre(),
                r.getFecha(),
                r.getHoraInicio(),
                r.getHoraFin(),
                r.getMonto(),
                r.getEstado(),
                estadoPago,
                r.isPagoRequerido(),
                r.getExpiresAt()
        );
    }
}
