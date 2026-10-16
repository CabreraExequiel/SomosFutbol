package com.canchasya.api.service;

import com.canchasya.api.dto.CiudadRequest;
import com.canchasya.api.dto.CiudadResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Ciudad;
import com.canchasya.api.repository.CiudadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CiudadService {

    private final CiudadRepository ciudadRepository;

    public List<CiudadResponse> listar() {
        return ciudadRepository.findAll().stream()
                .map(CiudadResponse::fromEntity)
                .toList();
    }

    public CiudadResponse crear(CiudadRequest request) {
        if (ciudadRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new ApiException("La ciudad ya existe", HttpStatus.CONFLICT);
        }
        Ciudad ciudad = new Ciudad();
        ciudad.setNombre(request.getNombre());
        ciudad.setProvincia(request.getProvincia());
        ciudadRepository.save(ciudad);
        return CiudadResponse.fromEntity(ciudad);
    }
}
