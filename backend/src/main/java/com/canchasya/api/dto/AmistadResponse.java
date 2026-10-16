package com.canchasya.api.dto;

import com.canchasya.api.model.Amistad;
import com.canchasya.api.model.EstadoAmistad;
import com.canchasya.api.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AmistadResponse {

    private Long id;
    private EstadoAmistad estado;
    private Long userId;
    private String nombre;
    private String avatar;
    private String ciudad;
    private boolean enviadaPorMi;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static AmistadResponse fromEntity(Amistad amistad, Long miUserId) {
        boolean enviadaPorMi = amistad.getSolicitante().getId().equals(miUserId);
        User otro = enviadaPorMi ? amistad.getDestinatario() : amistad.getSolicitante();

        return new AmistadResponse(
                amistad.getId(),
                amistad.getEstado(),
                otro.getId(),
                otro.getName(),
                otro.getAvatar(),
                otro.getCiudad(),
                enviadaPorMi,
                amistad.getCreatedAt(),
                amistad.getRespondedAt()
        );
    }
}
