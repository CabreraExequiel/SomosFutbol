package com.canchasya.api.dto;

import com.canchasya.api.model.Amistad;
import com.canchasya.api.model.EstadoAmistad;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioSocialResponse {

    private Long id;
    private String name;
    private String avatar;
    private String ciudad;
    private Role role;
    private String relacion;
    private Long solicitudId;

    public static UsuarioSocialResponse fromEntity(User user, Amistad amistad, Long miUserId) {
        String relacion = "NINGUNA";
        Long solicitudId = null;

        if (amistad != null) {
            solicitudId = amistad.getId();
            if (amistad.getEstado() == EstadoAmistad.ACEPTADA) {
                relacion = "AMIGOS";
            } else if (amistad.getEstado() == EstadoAmistad.PENDIENTE) {
                relacion = amistad.getSolicitante().getId().equals(miUserId)
                        ? "PENDIENTE_ENVIADA"
                        : "PENDIENTE_RECIBIDA";
            }
        }

        return new UsuarioSocialResponse(
                user.getId(),
                user.getName(),
                user.getAvatar(),
                user.getCiudad(),
                user.getRole(),
                relacion,
                solicitudId
        );
    }
}
