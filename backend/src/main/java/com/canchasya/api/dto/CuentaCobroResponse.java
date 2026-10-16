package com.canchasya.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CuentaCobroResponse {

    private String cbuCvu;
    private String aliasMercadoPago;
    private boolean configurada;
}
