package com.canchasya.api.service;

import com.canchasya.api.dto.PublicacionResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Publicacion;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PublicacionServiceHashtagTest {
    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final LikeRepository likes = mock(LikeRepository.class);
    private final PublicacionService servicio = new PublicacionService(
            publicaciones, mock(ComentarioRepository.class), likes,
            mock(UserRepository.class), mock(AmistadService.class),
            mock(AmistadRepository.class), mock(NotificacionService.class),
            mock(VideoStorageService.class));

    private User usuario() {
        User user = new User();
        user.setId(7L);
        return user;
    }

    @Test
    void buscaHashtagCompletoYPaginaEnLaBase() {
        User autor = usuario();
        Publicacion p = new Publicacion();
        p.setId(31L);
        p.setUser(autor);
        p.setContenido("Partido #futbol5 esta tarde");
        when(publicaciones.buscarPorHashtag(any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(p)));
        when(likes.idsLikeadosPorUsuario(7L, Set.of(31L))).thenReturn(Set.of(31L));

        List<PublicacionResponse> resultado = servicio.buscarPorHashtag(autor, "#futbol5", 2, 10);
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).isLikeadoPorMi()).isTrue();
        ArgumentCaptor<String> patron = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(publicaciones).buscarPorHashtag(patron.capture(), pagina.capture());
        assertThat(patron.getValue()).isEqualTo("(^|[^[:alnum:]_])#futbol5([^[:alnum:]_]|$)");
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(10);
        verify(likes).idsLikeadosPorUsuario(7L, Set.of(31L));
    }

    @Test
    void rechazaHashtagsInvalidosSinConsultarLaBase() {
        for (String etiqueta : List.of("", "#", "#a", "#futbol.*", "#hola mundo", "a".repeat(31))) {
            assertThatThrownBy(() -> servicio.buscarPorHashtag(usuario(), etiqueta, 0, 10))
                    .isInstanceOf(ApiException.class);
        }
        verifyNoInteractions(publicaciones);
    }

    @Test
    void respetaLimitesDePaginacionYNoFiltraDatosSinSesion() {
        assertThatThrownBy(() -> servicio.buscarPorHashtag(null, "futbol", 0, 10))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> servicio.buscarPorHashtag(usuario(), "futbol", -1, 10))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> servicio.buscarPorHashtag(usuario(), "futbol", 0, 51))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(publicaciones);
    }
}
