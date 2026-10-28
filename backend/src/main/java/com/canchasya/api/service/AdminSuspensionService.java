package com.canchasya.api.service;

import com.canchasya.api.model.BitacoraEvento;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.BitacoraRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSuspensionService {
    private final UserRepository users;
    private final BitacoraRepository bitacora;

    @Transactional
    public User actualizar(Long id, Long actorId, boolean suspender, String motivo) {
        if (id == null || actorId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (id.equals(actorId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés modificar tu propia cuenta");
        User actor = users.findById(actorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (actor.getRole() != Role.ADMINISTRADOR || !actor.isEnabled())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        User destino = users.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (destino.getRole() == Role.ADMINISTRADOR)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se pueden suspender administradores");
        String razon = motivo == null ? "" : motivo.strip();
        if (suspender && (razon.length() < 5 || razon.length() > 500))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indicá un motivo de 5 a 500 caracteres");
        if (destino.isSuspendido() == suspender) return destino;

        destino.setSuspendido(suspender);
        destino.setMotivoSuspension(suspender ? razon : null);
        destino.setSuspendidoEn(suspender ? Instant.now() : null);
        destino.setSuspendidoPorId(suspender ? actorId : null);
        users.save(destino);

        BitacoraEvento evento = new BitacoraEvento();
        evento.setFecha(Instant.now());
        evento.setNivel("INFO");
        evento.setOrigen("BACKEND");
        evento.setAmbiente("administracion");
        evento.setTipo(suspender ? "USUARIO_SUSPENDIDO" : "USUARIO_REACTIVADO");
        evento.setCodigo("SF-" + UUID.randomUUID());
        evento.setDescripcion(suspender ? "Cuenta suspendida por administrador" : "Cuenta reactivada por administrador");
        evento.setActorId(actorId);
        evento.setReferenciaTipo("USUARIO");
        evento.setReferenciaId(destino.getId());
        evento.setRuta("/api/admin/usuarios/" + destino.getId() + "/estado");
        bitacora.save(evento);
        return destino;
    }
}
