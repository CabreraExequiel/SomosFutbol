package com.canchasya.api.dto;

import com.canchasya.api.model.Ciudad;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CiudadResponse {
    private Long id;
    private String nombre;
    private String provincia;

    public static CiudadResponse fromEntity(Ciudad ciudad) {
        return new CiudadResponse(ciudad.getId(), ciudad.getNombre(), ciudad.getProvincia());
    }
}
