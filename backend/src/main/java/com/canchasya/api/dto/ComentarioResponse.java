package com.canchasya.api.dto;

import com.canchasya.api.model.Comentario;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ComentarioResponse {

    private Long id;
    private Long userId;
    private String userNombre;
    private String userAvatar;
    private String contenido;
    private LocalDateTime createdAt;

    public static ComentarioResponse fromEntity(Comentario c) {
        return new ComentarioResponse(
                c.getId(),
                c.getUser().getId(),
                c.getUser().getName(),
                c.getUser().getAvatar(),
                c.getContenido(),
                c.getCreatedAt()
        );
    }
}
