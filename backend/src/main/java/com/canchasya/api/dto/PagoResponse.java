package com.canchasya.api.dto;

import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.model.Pago;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PagoResponse {

    private Long id;
    private Long reservaId;
    private BigDecimal monto;
    private EstadoPago estado;
    private String initPoint;
    private String mpPaymentId;
    private LocalDateTime fechaPago;

    public static PagoResponse fromEntity(Pago pago, String initPoint) {
        return new PagoResponse(
                pago.getId(),
                pago.getReserva().getId(),
                pago.getMonto(),
                pago.getEstado(),
                initPoint,
                pago.getMpPaymentId(),
                pago.getFechaPago()
        );
    }

    public static PagoResponse fromEntity(Pago pago) {
        return fromEntity(pago, null);
    }
}
