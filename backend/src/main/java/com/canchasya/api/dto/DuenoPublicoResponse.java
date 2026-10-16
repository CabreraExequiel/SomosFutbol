package com.canchasya.api.dto;
import java.math.BigDecimal;
import java.util.List;
public record DuenoPublicoResponse(Long id, String nombre, String avatar, String imagen,
    int cantidadCanchas, BigDecimal precioDesde, List<String> ciudades, List<String> tipos) {}
