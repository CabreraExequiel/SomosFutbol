package com.canchasya.api.service;

import com.canchasya.api.dto.AmistadResponse;
import com.canchasya.api.dto.UsuarioSocialResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.AmistadRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AmistadService {

    private final AmistadRepository amistadRepository;
    private final UserRepository userRepository;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<AmistadResponse> listarAmigos(User user) {
        return amistadRepository.findRelaciones(user.getId(), EstadoAmistad.ACEPTADA)
                .stream()
                .map(a -> AmistadResponse.fromEntity(a, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AmistadResponse> solicitudesRecibidas(User user) {
        return amistadRepository
                .findByDestinatarioIdAndEstadoOrderByCreatedAtDesc(user.getId(), EstadoAmistad.PENDIENTE)
                .stream()
                .map(a -> AmistadResponse.fromEntity(a, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AmistadResponse> solicitudesEnviadas(User user) {
        return amistadRepository
                .findBySolicitanteIdAndEstadoOrderByCreatedAtDesc(user.getId(), EstadoAmistad.PENDIENTE)
                .stream()
                .map(a -> AmistadResponse.fromEntity(a, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioSocialResponse> buscarUsuarios(User user, String query) {
        String texto = query == null ? "" : query.trim();
        return userRepository.buscarParaComunidad(user.getId(), texto, PageRequest.of(0, 25))
                .stream()
                .map(candidato -> UsuarioSocialResponse.fromEntity(
                        candidato,
                        amistadRepository.findByPairKey(pairKey(user.getId(), candidato.getId())).orElse(null),
                        user.getId()
                ))
                .toList();
    }

    @Transactional
    public AmistadResponse enviarSolicitud(Long destinatarioId, User solicitante) {
        if (solicitante.getId().equals(destinatarioId)) {
            throw new ApiException("No podés enviarte una solicitud a vos mismo", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User destinatario = userRepository.findById(destinatarioId)
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        String pairKey = pairKey(solicitante.getId(), destinatarioId);
        Amistad amistad = amistadRepository.findByPairKey(pairKey).orElse(null);

        if (amistad != null && amistad.getEstado() == EstadoAmistad.ACEPTADA) {
            throw new ApiException("Este usuario ya es tu amigo", HttpStatus.CONFLICT);
        }
        if (amistad != null && amistad.getEstado() == EstadoAmistad.PENDIENTE) {
            throw new ApiException("Ya existe una solicitud pendiente", HttpStatus.CONFLICT);
        }

        if (amistad == null) {
            amistad = new Amistad();
            amistad.setPairKey(pairKey);
        }
        amistad.setSolicitante(solicitante);
        amistad.setDestinatario(destinatario);
        amistad.setEstado(EstadoAmistad.PENDIENTE);
        amistad.setRespondedAt(null);

        try {
            amistadRepository.saveAndFlush(amistad);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException("Ya existe una solicitud entre estos usuarios", HttpStatus.CONFLICT);
        }

        notificacionService.crear(
                destinatario,
                solicitante,
                TipoNotificacion.SOLICITUD_AMISTAD,
                "Nueva solicitud de amistad",
                solicitante.getName() + " quiere agregarte como amigo.",
                "/amigos?seccion=solicitudes",
                amistad.getId()
        );

        return AmistadResponse.fromEntity(amistad, solicitante.getId());
    }

    @Transactional
    public AmistadResponse aceptar(Long solicitudId, User user) {
        Amistad amistad = obtenerSolicitudRecibida(solicitudId, user);
        amistad.setEstado(EstadoAmistad.ACEPTADA);
        amistad.setRespondedAt(LocalDateTime.now());
        amistadRepository.save(amistad);

        notificacionService.marcarPorReferencia(user, TipoNotificacion.SOLICITUD_AMISTAD, amistad.getId());
        notificacionService.crear(
                amistad.getSolicitante(),
                user,
                TipoNotificacion.AMISTAD_ACEPTADA,
                "Solicitud aceptada",
                user.getName() + " aceptó tu solicitud de amistad.",
                "/amigos",
                amistad.getId()
        );

        return AmistadResponse.fromEntity(amistad, user.getId());
    }

    @Transactional
    public void rechazar(Long solicitudId, User user) {
        Amistad amistad = obtenerSolicitudRecibida(solicitudId, user);
        amistad.setEstado(EstadoAmistad.RECHAZADA);
        amistad.setRespondedAt(LocalDateTime.now());
        amistadRepository.save(amistad);
        notificacionService.marcarPorReferencia(user, TipoNotificacion.SOLICITUD_AMISTAD, amistad.getId());
    }

    @Transactional
    public void eliminarAmigo(Long amigoId, User user) {
        Amistad amistad = amistadRepository.findByPairKey(pairKey(user.getId(), amigoId))
                .orElseThrow(() -> new ApiException("Amistad no encontrada", HttpStatus.NOT_FOUND));

        if (amistad.getEstado() != EstadoAmistad.ACEPTADA) {
            throw new ApiException("La relación no es una amistad activa", HttpStatus.CONFLICT);
        }
        amistadRepository.delete(amistad);
    }

    @Transactional(readOnly = true)
    public boolean sonAmigos(Long userId, Long otroUserId) {
        return amistadRepository.findByPairKey(pairKey(userId, otroUserId))
                .map(a -> a.getEstado() == EstadoAmistad.ACEPTADA)
                .orElse(false);
    }

    private Amistad obtenerSolicitudRecibida(Long solicitudId, User user) {
        Amistad amistad = amistadRepository.findById(solicitudId)
                .orElseThrow(() -> new ApiException("Solicitud no encontrada", HttpStatus.NOT_FOUND));

        if (!amistad.getDestinatario().getId().equals(user.getId())) {
            throw new ApiException("No tenés permiso sobre esta solicitud", HttpStatus.FORBIDDEN);
        }
        if (amistad.getEstado() != EstadoAmistad.PENDIENTE) {
            throw new ApiException("La solicitud ya fue respondida", HttpStatus.CONFLICT);
        }
        return amistad;
    }

    private String pairKey(Long uno, Long dos) {
        return Math.min(uno, dos) + ":" + Math.max(uno, dos);
    }
}
