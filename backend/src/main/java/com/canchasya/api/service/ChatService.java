package com.canchasya.api.service;

import com.canchasya.api.dto.*;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Conversacion;
import com.canchasya.api.model.Mensaje;
import com.canchasya.api.model.TipoNotificacion;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.ConversacionRepository;
import com.canchasya.api.repository.MensajeRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final UserRepository userRepository;
    private final AmistadService amistadService;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<ConversacionResponse> misConversaciones(User user) {
        return conversacionRepository.findByUserUnoIdOrUserDosIdOrderByUpdatedAtDesc(user.getId(), user.getId())
                .stream()
                .map(c -> toResponse(c, user.getId()))
                .toList();
    }

    @Transactional
    public ConversacionResponse iniciar(IniciarConversacionRequest request, User user) {
        if (request.getOtroUserId().equals(user.getId())) {
            throw new ApiException("No podés iniciar una conversación con vos mismo", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        User otro = userRepository.findById(request.getOtroUserId())
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        var existente = conversacionRepository.findByUserUnoIdAndUserDosId(user.getId(), otro.getId())
                .or(() -> conversacionRepository.findByUserUnoIdAndUserDosId(otro.getId(), user.getId()));

        if (existente.isEmpty() && !amistadService.sonAmigos(user.getId(), otro.getId())) {
            throw new ApiException("Solo podés iniciar conversaciones con tus amigos", HttpStatus.FORBIDDEN);
        }

        Conversacion conversacion = existente.orElseGet(() -> {
            Conversacion nueva = new Conversacion();
            if (user.getId() < otro.getId()) {
                nueva.setUserUno(user);
                nueva.setUserDos(otro);
            } else {
                nueva.setUserUno(otro);
                nueva.setUserDos(user);
            }
            return conversacionRepository.save(nueva);
        });

        return toResponse(conversacion, user.getId());
    }

    @Transactional
    public List<MensajeResponse> mensajes(Long conversacionId, User user) {
        Conversacion conversacion = obtenerYValidar(conversacionId, user);
        mensajeRepository.marcarComoLeidos(conversacion.getId(), user.getId());
        notificacionService.marcarPorReferencia(user, TipoNotificacion.MENSAJE, conversacion.getId());
        return mensajeRepository.findByConversacionIdOrderByCreatedAtAsc(conversacion.getId())
                .stream()
                .map(MensajeResponse::fromEntity)
                .toList();
    }

    @Transactional
    public MensajeResponse enviar(Long conversacionId, MensajeRequest request, User user) {
        Conversacion conversacion = obtenerYValidar(conversacionId, user);

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setUser(user);
        mensaje.setContenido(request.getContenido().trim());
        mensajeRepository.save(mensaje);

        conversacion.setUpdatedAt(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        User destinatario = conversacion.getUserUno().getId().equals(user.getId())
                ? conversacion.getUserDos()
                : conversacion.getUserUno();
        notificacionService.crear(
                destinatario,
                user,
                TipoNotificacion.MENSAJE,
                "Nuevo mensaje",
                user.getName() + " te envió un mensaje.",
                "/chat?conversacion=" + conversacion.getId(),
                conversacion.getId()
        );

        return MensajeResponse.fromEntity(mensaje);
    }

    private ConversacionResponse toResponse(Conversacion conversacion, Long miUserId) {
        Mensaje ultimo = mensajeRepository
                .findTopByConversacionIdOrderByCreatedAtDesc(conversacion.getId())
                .orElse(null);
        long noLeidos = mensajeRepository
                .countByConversacionIdAndUserIdNotAndLeidoFalse(conversacion.getId(), miUserId);

        return ConversacionResponse.fromEntity(
                conversacion,
                miUserId,
                ultimo != null ? ultimo.getContenido() : null,
                ultimo != null ? ultimo.getCreatedAt() : null,
                noLeidos
        );
    }

    private Conversacion obtenerYValidar(Long id, User user) {
        Conversacion conversacion = conversacionRepository.findById(id)
                .orElseThrow(() -> new ApiException("Conversación no encontrada", HttpStatus.NOT_FOUND));

        boolean participa = conversacion.getUserUno().getId().equals(user.getId())
                || conversacion.getUserDos().getId().equals(user.getId());

        if (!participa) {
            throw new ApiException("No tenés acceso a esta conversación", HttpStatus.FORBIDDEN);
        }
        return conversacion;
    }
}
