package com.canchasya.api.controller;

import com.canchasya.api.dto.*;
import com.canchasya.api.model.User;
import com.canchasya.api.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionResponse>> misConversaciones(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.misConversaciones(user));
    }

    @PostMapping("/conversaciones")
    public ResponseEntity<ConversacionResponse> iniciar(@Valid @RequestBody IniciarConversacionRequest request,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.iniciar(request, user));
    }

    @GetMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<List<MensajeResponse>> mensajes(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.mensajes(id, user));
    }

    @PostMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<MensajeResponse> enviar(@PathVariable Long id,
                                                    @Valid @RequestBody MensajeRequest request,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.enviar(id, request, user));
    }
}
