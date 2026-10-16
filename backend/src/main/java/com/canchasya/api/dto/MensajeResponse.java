package com.canchasya.api.dto;

import com.canchasya.api.model.Mensaje;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MensajeResponse {

    private Long id;
    private Long userId;
    private String contenido;
    private boolean leido;
    private LocalDateTime createdAt;

    public static MensajeResponse fromEntity(Mensaje m) {
        return new MensajeResponse(m.getId(), m.getUser().getId(), m.getContenido(), m.isLeido(), m.getCreatedAt());
    }
}
