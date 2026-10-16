package com.canchasya.api.dto;

import com.canchasya.api.model.Notificacion;
import com.canchasya.api.model.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificacionResponse {

    private Long id;
    private TipoNotificacion tipo;
    private String titulo;
    private String mensaje;
    private String destino;
    private Long referenciaId;
    private boolean leida;
    private LocalDateTime createdAt;
    private Long actorId;
    private String actorNombre;
    private String actorAvatar;

    public static NotificacionResponse fromEntity(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getTipo(),
                n.getTitulo(),
                n.getMensaje(),
                n.getDestino(),
                n.getReferenciaId(),
                n.isLeida(),
                n.getCreatedAt(),
                n.getActor() != null ? n.getActor().getId() : null,
                n.getActor() != null ? n.getActor().getName() : null,
                n.getActor() != null ? n.getActor().getAvatar() : null
        );
    }
}
