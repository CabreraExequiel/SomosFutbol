package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Cancha;
import com.canchasya.api.repository.CanchaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminCanchaModeracionService {
    private final CanchaRepository canchas;
    private final BitacoraService bitacora;

    @Transactional
    public Cancha actualizar(Long id, Long adminId, boolean activa, String motivo) {
        Cancha cancha = canchas.findByIdForUpdate(id)
            .orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));
        if (cancha.isActiva() == activa)
            throw new ApiException("La cancha ya tiene ese estado", HttpStatus.CONFLICT);
        String limpio = motivo == null ? "" : motivo.strip();
        if (!activa && (limpio.length() < 5 || limpio.length() > 500))
            throw new ApiException("Indicá un motivo entre 5 y 500 caracteres", HttpStatus.BAD_REQUEST);
        cancha.setActiva(activa);
        cancha.setMotivoDesactivacionAdmin(activa ? null : limpio);
        cancha.setModeracionAdminId(adminId);
        cancha.setModeracionAdminFecha(Instant.now());
        canchas.saveAndFlush(cancha);
        bitacora.evento(activa ? "CANCHA_REACTIVADA_ADMIN" : "CANCHA_DESACTIVADA_ADMIN", "CANCHA", id, adminId);
        return cancha;
    }
}
