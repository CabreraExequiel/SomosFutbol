package com.canchasya.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CuentaCobroRequest {

    @Size(max = 30, message = "El CBU/CVU no puede superar los 30 caracteres")
    @Pattern(
            regexp = "^\\s*$|^(?:[0-9][ -]?){21}[0-9]$",
            message = "El CBU/CVU debe contener exactamente 22 dígitos"
    )
    private String cbuCvu;

    @Pattern(
            regexp = "^\\s*$|^[A-Za-z0-9.-]{6,20}$",
            message = "El alias debe tener entre 6 y 20 caracteres y usar solo letras, números, puntos o guiones"
    )
    private String aliasMercadoPago;

    @AssertTrue(message = "Informá un CBU/CVU o alias de Mercado Pago")
    public boolean isCuentaInformada() {
        return tieneTexto(cbuCvu) || tieneTexto(aliasMercadoPago);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
