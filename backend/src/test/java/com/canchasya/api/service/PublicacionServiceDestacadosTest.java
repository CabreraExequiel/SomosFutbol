package com.canchasya.api.service;

import com.canchasya.api.dto.HashtagDestacadoResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PublicacionServiceDestacadosTest {
    private final PublicacionRepository publicaciones = mock(PublicacionRepository.class);
    private final PublicacionService servicio = new PublicacionService(
            publicaciones, mock(ComentarioRepository.class), mock(LikeRepository.class),
            mock(UserRepository.class), mock(AmistadService.class),
            mock(AmistadRepository.class), mock(NotificacionService.class),
            mock(VideoStorageService.class));

    private User usuario() {
        User u = new User();
        u.setId(4L);
        return u;
    }

    @Test
    void destacaEtiquetasPorPublicacionesUnicasYNormalizaMayusculas() {
        when(publicaciones.listarContenidosEtiquetadosRecientes(any(), any(Pageable.class)))
                .thenReturn(List.of(
                        "Hoy #futbol5 #FUTBOL5 #partido",
                        "Manana #Futbol5 #liga",
                        "Nueva #liga y #partido",
                        "texto #futbol5extra #x #futbol5abcdefghijklmnopqrstuvwx"
                ));

        List<HashtagDestacadoResponse> resultado = servicio.listarHashtagsDestacados(usuario());
        assertThat(resultado).containsExactly(
                new HashtagDestacadoResponse("futbol5", 2),
                new HashtagDestacadoResponse("liga", 2),
                new HashtagDestacadoResponse("partido", 2),
                new HashtagDestacadoResponse("futbol5extra", 1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(publicaciones).listarContenidosEtiquetadosRecientes(any(LocalDateTime.class), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(500);
    }

    @Test
    void sinSesionNoConsultaLaBase() {
        assertThatThrownBy(() -> servicio.listarHashtagsDestacados(null))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(publicaciones);
    }
}
