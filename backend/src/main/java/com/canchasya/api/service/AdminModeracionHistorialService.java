package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.AdminModeracionRegistro;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.AdminModeracionRegistroRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 09C5: los cambios y su historial se confirman o revierten juntos. */
@Service
@RequiredArgsConstructor
public class AdminModeracionHistorialService {
    private final AdminModeracionRegistroRepository registros;
    private final UserRepository users;

    public record RegistroRespuesta(Long id, Instant fecha, String referenciaTipo, Long referenciaId,
                                    String accion, String estadoAnterior, String estadoNuevo,
                                    String motivo, Long administradorId, String administradorNombre) {
        static RegistroRespuesta de(AdminModeracionRegistro x) {
            return new RegistroRespuesta(x.getId(), x.getFecha(), x.getReferenciaTipo(),
                    x.getReferenciaId(), x.getAccion(), x.getEstadoAnterior(), x.getEstadoNuevo(),
                    x.getMotivo(), x.getAdministradorId(), x.getAdministradorNombre());
        }
    }

    /** Debe invocarse DENTRO de la misma transaccion que actualiza la cancha o el complejo. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String tipo, Long referenciaId, Long adminId, String accion,
                          String anterior, String nuevo, String motivo) {
        User administrador = users.findById(adminId)
                .orElseThrow(() -> new ApiException("Administrador no encontrado", HttpStatus.CONFLICT));
        AdminModeracionRegistro registro = new AdminModeracionRegistro();
        registro.setFecha(Instant.now());
        registro.setReferenciaTipo(tipo);
        registro.setReferenciaId(referenciaId);
        registro.setAccion(accion);
        registro.setEstadoAnterior(anterior);
        registro.setEstadoNuevo(nuevo);
        registro.setMotivo(motivo == null || motivo.isBlank() ? null : motivo);
        registro.setAdministradorId(adminId);
        String nombre = administrador.getName() == null ? "Administrador" : administrador.getName();
        registro.setAdministradorNombre(nombre.substring(0, Math.min(150, nombre.length())));
        registros.saveAndFlush(registro);
    }

    @Transactional(readOnly = true)
    public Page<RegistroRespuesta> listar(String tipo, Long referenciaId, int pagina) {
        if (referenciaId == null || referenciaId <= 0 || pagina < 0 || pagina > 10000) {
            throw new ApiException("Consulta de historial invalida", HttpStatus.BAD_REQUEST);
        }
        return registros.findByReferenciaTipoAndReferenciaId(tipo, referenciaId,
                        PageRequest.of(pagina, 10, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))))
                .map(RegistroRespuesta::de);
    }
}
