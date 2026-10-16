package com.canchasya.api.dto;

import com.canchasya.api.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioMencionResponse {
    private Long id;
    private String name;

    public static UsuarioMencionResponse fromEntity(User user) {
        return new UsuarioMencionResponse(user.getId(), user.getName());
    }
}
