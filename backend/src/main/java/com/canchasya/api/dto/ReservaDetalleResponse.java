package com.canchasya.api.dto;
import com.canchasya.api.dto.auth.UserResponse;
public record ReservaDetalleResponse(ReservaResponse reserva, UserResponse usuario, String direccion, String ciudadCancha) {}
