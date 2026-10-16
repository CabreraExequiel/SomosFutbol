package com.canchasya.api.dto;

import com.canchasya.api.model.Cancha;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CanchaResponse {

    private Long id;
    private Long duenoId;
    private String duenoNombre;
    private Long ciudadId;
    private String ciudadNombre;
    private String nombre;
    private String direccion;
    private String tipo;
    private String superficie;
    private Integer cantJugadores;
    private BigDecimal precioHora;
    private boolean techada;
    private String imagen;
    private String descripcion;
    private boolean activa;

    public static CanchaResponse fromEntity(Cancha c) {
        return new CanchaResponse(
                c.getId(),
                c.getDueno().getId(),
                c.getDueno().getName(),
                c.getCiudad().getId(),
                c.getCiudad().getNombre(),
                c.getNombre(),
                c.getDireccion(),
                c.getTipo(),
                c.getSuperficie(),
                c.getCantJugadores(),
                c.getPrecioHora(),
                c.isTechada(),
                c.getImagen(),
                c.getDescripcion(),
                c.isActiva()
        );
    }
}
