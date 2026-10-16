package com.canchasya.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DatosTransferenciaResponse {

    private Long reservaId;
    private String canchaNombre;
    private String duenoNombre;
    private BigDecimal monto;
    private String cbuCvu;
    private String aliasMercadoPago;
}
