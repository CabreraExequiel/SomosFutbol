package com.canchasya.api.dto.auth;

import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String telefono;
    private String ciudad;
    private Role role;
    private String avatar;
    private String nombreComplejo;

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTelefono(),
                user.getCiudad(),
                user.getRole(),
                user.getAvatar(),
                user.getNombreComplejo()
        );
    }
}
