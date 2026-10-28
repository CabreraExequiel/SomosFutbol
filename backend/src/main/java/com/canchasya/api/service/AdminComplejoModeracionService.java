package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Cancha;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.CanchaRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Suspensión por propietario: conserva intactos los estados individuales de canchas. */
@Service
@RequiredArgsConstructor
public class AdminComplejoModeracionService {
    private final UserRepository users;
    private final CanchaRepository canchas;
    private final BitacoraService bitacora;

    public record ComplejoAdminDetalle(Long propietarioId, String nombreComplejo, String propietario,
                                        boolean suspendido, String motivoSuspension,
                                        long canchasTotal, long canchasActivas) {}

    @Transactional(readOnly = true)
    public ComplejoAdminDetalle detalle(Long duenoId) {
        User dueno = dueno(duenoId, false);
        return respuesta(dueno);
    }

    @Transactional
    public ComplejoAdminDetalle actualizar(Long duenoId, Long adminId, boolean suspender, String motivo) {
        // La operación solo bloquea al propietario; nunca modifica las canchas individuales.
        User dueno = dueno(duenoId, true);
        if (dueno.isComplejoSuspendido() == suspender)
            throw new ApiException("El complejo ya tiene ese estado", HttpStatus.CONFLICT);
        String limpio = motivo == null ? "" : motivo.strip();
        if (suspender && (limpio.length() < 5 || limpio.length() > 500))
            throw new ApiException("Indicá un motivo entre 5 y 500 caracteres", HttpStatus.BAD_REQUEST);
        dueno.setComplejoSuspendido(suspender);
        dueno.setComplejoMotivoSuspension(suspender ? limpio : null);
        dueno.setComplejoSuspendidoEn(suspender ? Instant.now() : null);
        dueno.setComplejoSuspendidoPorId(suspender ? adminId : null);
        users.saveAndFlush(dueno);
        bitacora.evento(suspender ? "COMPLEJO_SUSPENDIDO_ADMIN" : "COMPLEJO_REACTIVADO_ADMIN",
                "COMPLEJO", duenoId, adminId);
        return respuesta(dueno);
    }

    private User dueno(Long id, boolean bloquear) {
        User u = (bloquear ? users.findByIdForUpdate(id) : users.findById(id))
                .orElseThrow(() -> new ApiException("Propietario no encontrado", HttpStatus.NOT_FOUND));
        // Nunca aplicar este flujo sobre administradores.
        if (u.getRole() != Role.DUENO)
            throw new ApiException("El usuario no es propietario de un complejo", HttpStatus.CONFLICT);
        return u;
    }

    private ComplejoAdminDetalle respuesta(User dueno) {
        List<Cancha> lista = canchas.findByDuenoId(dueno.getId());
        String comercial = dueno.getNombreComplejo();
        return new ComplejoAdminDetalle(dueno.getId(),
                comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                dueno.getName(), dueno.isComplejoSuspendido(), dueno.getComplejoMotivoSuspension(),
                lista.size(), lista.stream().filter(Cancha::isActiva).count());
    }
}
