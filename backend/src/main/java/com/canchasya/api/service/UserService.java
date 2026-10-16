package com.canchasya.api.service;

import com.canchasya.api.dto.UserUpdateRequest;
import com.canchasya.api.dto.auth.UserResponse;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ArchivoService archivoService;

    public List<UserResponse> listar() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional
    public UserResponse actualizar(UserUpdateRequest request, User user) {
        if (request.getName() != null) user.setName(request.getName().trim());
        user.setTelefono(request.getTelefono().trim());
        if (request.getCiudad() != null) user.setCiudad(request.getCiudad().trim());
        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse actualizarAvatar(MultipartFile archivo, User user) {
        String anterior = user.getAvatar();
        String url = archivoService.guardarImagen(archivo, "avatars");
        user.setAvatar(url);
        userRepository.save(user);
        archivoService.eliminarSiEsLocal(anterior);
        return UserResponse.fromEntity(user);
    }
}
