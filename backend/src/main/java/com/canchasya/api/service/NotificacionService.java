package com.canchasya.api.service;

import com.canchasya.api.dto.NotificacionResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Notificacion;
import com.canchasya.api.model.TipoNotificacion;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(User user, int limit) {
        int limiteSeguro = Math.max(1, Math.min(limit, 100));
        return notificacionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, limiteSeguro))
                .stream()
                .map(NotificacionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(User user) {
        return notificacionRepository.countByUserIdAndLeidaFalse(user.getId());
    }

    @Transactional
    public NotificacionResponse marcarLeida(Long id, User user) {
        Notificacion notificacion = obtenerPropia(id, user);
        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
        }
        return NotificacionResponse.fromEntity(notificacion);
    }

    @Transactional
    public void marcarTodas(User user) {
        notificacionRepository.marcarTodasLeidas(user.getId());
    }

    @Transactional
    public void eliminar(Long id, User user) {
        notificacionRepository.delete(obtenerPropia(id, user));
    }

    @Transactional
    public void crear(
            User destinatario,
            User actor,
            TipoNotificacion tipo,
            String titulo,
            String mensaje,
            String destino,
            Long referenciaId
    ) {
        if (destinatario == null) return;
        if (actor != null && destinatario.getId().equals(actor.getId())) return;

        Notificacion notificacion = new Notificacion();
        notificacion.setUser(destinatario);
        notificacion.setActor(actor);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(recortar(titulo, 120));
        notificacion.setMensaje(recortar(mensaje, 500));
        notificacion.setDestino(destino == null ? null : recortar(destino, 300));
        notificacion.setReferenciaId(referenciaId);
        notificacionRepository.save(notificacion);
    }

    @Transactional
    public void marcarPorReferencia(User user, TipoNotificacion tipo, Long referenciaId) {
        if (referenciaId != null) {
            notificacionRepository.marcarPorReferencia(user.getId(), tipo, referenciaId);
        }
    }

    private Notificacion obtenerPropia(Long id, User user) {
        return notificacionRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ApiException("Notificación no encontrada", HttpStatus.NOT_FOUND));
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) return "";
        String limpio = valor.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}
