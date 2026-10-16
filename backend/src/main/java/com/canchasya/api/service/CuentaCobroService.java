package com.canchasya.api.service;

import com.canchasya.api.dto.CuentaCobroRequest;
import com.canchasya.api.dto.CuentaCobroResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CuentaCobroService {

    private final UserRepository userRepository;

    public CuentaCobroResponse obtener(User user) {
        verificarDueno(user);
        return respuesta(user);
    }

    @Transactional
    public CuentaCobroResponse actualizar(CuentaCobroRequest request, User user) {
        verificarDueno(user);
        CuentaNormalizada cuenta = normalizarYValidar(request.getCbuCvu(), request.getAliasMercadoPago());
        user.setCbuCvu(cuenta.cbuCvu());
        user.setAliasMercadoPago(cuenta.aliasMercadoPago());
        userRepository.save(user);
        return respuesta(user);
    }

    public CuentaNormalizada normalizarYValidar(String cbuCvu, String aliasMercadoPago) {
        String cbuNormalizado = normalizarCbuCvu(cbuCvu);
        String aliasNormalizado = normalizarAlias(aliasMercadoPago);

        if (cbuNormalizado == null && aliasNormalizado == null) {
            throw new ApiException("Informá un CBU/CVU o alias de Mercado Pago", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return new CuentaNormalizada(cbuNormalizado, aliasNormalizado);
    }

    private String normalizarCbuCvu(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String normalizado = valor.replaceAll("[\\s-]", "");
        if (!normalizado.matches("\\d{22}")) {
            throw new ApiException("El CBU/CVU debe contener exactamente 22 dígitos", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return normalizado;
    }

    private String normalizarAlias(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        if (!normalizado.matches("[a-z0-9.-]{6,20}")) {
            throw new ApiException(
                    "El alias debe tener entre 6 y 20 caracteres y usar solo letras, números, puntos o guiones",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
        return normalizado;
    }

    private void verificarDueno(User user) {
        if (user.getRole() != Role.DUENO) {
            throw new ApiException("La cuenta de cobro solo está disponible para dueños", HttpStatus.FORBIDDEN);
        }
    }

    private CuentaCobroResponse respuesta(User user) {
        boolean configurada = user.getCbuCvu() != null || user.getAliasMercadoPago() != null;
        return new CuentaCobroResponse(user.getCbuCvu(), user.getAliasMercadoPago(), configurada);
    }

    public record CuentaNormalizada(String cbuCvu, String aliasMercadoPago) {}
}
