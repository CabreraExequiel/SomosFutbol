package com.canchasya.api.dto;

import com.canchasya.api.model.Conversacion;
import com.canchasya.api.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ConversacionResponse {

    private Long id;
    private Long otroUserId;
    private String otroUserNombre;
    private String otroUserAvatar;
    private LocalDateTime updatedAt;
    private String ultimoMensaje;
    private LocalDateTime ultimoMensajeAt;
    private long noLeidos;

    public static ConversacionResponse fromEntity(
            Conversacion c,
            Long miUserId,
            String ultimoMensaje,
            LocalDateTime ultimoMensajeAt,
            long noLeidos
    ) {
        User otro = c.getUserUno().getId().equals(miUserId) ? c.getUserDos() : c.getUserUno();
        return new ConversacionResponse(
                c.getId(),
                otro.getId(),
                otro.getName(),
                otro.getAvatar(),
                c.getUpdatedAt(),
                ultimoMensaje,
                ultimoMensajeAt,
                noLeidos
        );
    }
}
