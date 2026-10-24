package com.canchasya.api.service;

import com.canchasya.api.dto.PublicacionResponse;
import com.canchasya.api.model.EstadoAmistad;
import com.canchasya.api.model.Publicacion;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.AmistadRepository;
import com.canchasya.api.repository.ComentarioRepository;
import com.canchasya.api.repository.LikeRepository;
import com.canchasya.api.repository.PublicacionRepository;
import com.canchasya.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PublicacionServiceFeedTest {

    @Test
    void consultaLosLikesDeTodaLaPaginaEnUnSoloLote() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        ComentarioRepository comentarios = mock(ComentarioRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        AmistadRepository amistades = mock(AmistadRepository.class);

        PublicacionService servicio = new PublicacionService(
                publicaciones, comentarios, likes,
                mock(UserRepository.class), mock(AmistadService.class),
                amistades, mock(NotificacionService.class),
                mock(VideoStorageService.class)
        );

        User usuario = new User();
        usuario.setId(7L);
        usuario.setCiudad("Córdoba");

        Publicacion primera = new Publicacion();
        primera.setId(100L);
        primera.setUser(usuario);
        primera.setCreatedAt(LocalDateTime.of(2026, 10, 24, 9, 0));

        Publicacion segunda = new Publicacion();
        segunda.setId(200L);
        segunda.setUser(usuario);
        segunda.setCreatedAt(LocalDateTime.of(2026, 10, 23, 9, 0));

        when(publicaciones.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(primera, segunda)));
        when(amistades.findRelaciones(7L, EstadoAmistad.ACEPTADA))
                .thenReturn(List.of());
        when(likes.idsLikeadosPorUsuario(eq(7L), eq(Set.of(100L, 200L))))
                .thenReturn(Set.of(100L));

        List<PublicacionResponse> resultado = servicio.feed(usuario, 0, 10, "recientes");

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).isLikeadoPorMi()).isTrue();
        assertThat(resultado.get(1).isLikeadoPorMi()).isFalse();
        verify(likes, times(1)).idsLikeadosPorUsuario(7L, Set.of(100L, 200L));
        verify(likes, never()).findByPublicacionIdAndUserId(any(), any());
    }
}
