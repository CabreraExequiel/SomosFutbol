package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Cancha;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.CanchaRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Usar desde métodos @Transactional: bloqueo del dueño ANTES que el de la cancha. */
@Component
@RequiredArgsConstructor
public class ComplejoOperacionGuard {
    private final CanchaRepository canchas;
    private final UserRepository users;

    public User bloquearDueno(Long id) {
        User dueno = users.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException("Propietario no encontrado", HttpStatus.NOT_FOUND));
        if (dueno.isComplejoSuspendido())
            throw new ApiException("El complejo está suspendido por administración", HttpStatus.CONFLICT);
        return dueno;
    }

    public Cancha bloquearCanchaDeDueno(Long canchaId, User actor) {
        Long duenoId = canchas.findDuenoIdByCanchaId(canchaId)
                .orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));
        if (!duenoId.equals(actor.getId()))
            throw new ApiException("No tenés permiso sobre esta cancha", HttpStatus.FORBIDDEN);
        bloquearDueno(duenoId);
        Cancha cancha = canchas.findByIdForUpdate(canchaId)
                .orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));
        if (!cancha.getDueno().getId().equals(duenoId))
            throw new ApiException("Cambió el propietario de la cancha", HttpStatus.CONFLICT);
        return cancha;
    }
}
