package com.canchasya.api.service;

import com.canchasya.api.dto.PublicacionResponse;
import com.canchasya.api.model.Publicacion;
import com.canchasya.api.model.Amistad;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.AmistadRepository;
import com.canchasya.api.repository.ComentarioRepository;
import com.canchasya.api.repository.LikeRepository;
import com.canchasya.api.repository.PublicacionRepository;
import com.canchasya.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.PageImpl;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import com.canchasya.api.model.EstadoAmistad;

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

        when(publicaciones.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(primera, segunda)));
        when(likes.idsLikeadosPorUsuario(eq(7L), eq(Set.of(100L, 200L))))
                .thenReturn(Set.of(100L));

        List<PublicacionResponse> resultado = servicio.feed(usuario, 0, 10, "recientes");

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).isLikeadoPorMi()).isTrue();
        assertThat(resultado.get(1).isLikeadoPorMi()).isFalse();
        verify(likes, times(1)).idsLikeadosPorUsuario(7L, Set.of(100L, 200L));
        verify(likes, never()).findByPublicacionIdAndUserId(any(), any());
    }

    @Test
    void recientesPuedeConsultarPaginasPosterioresALos500Candidatos() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        AmistadRepository amistades = mock(AmistadRepository.class);
        PublicacionService servicio = new PublicacionService(
                publicaciones, mock(ComentarioRepository.class), likes,
                mock(UserRepository.class), mock(AmistadService.class),
                amistades, mock(NotificacionService.class),
                mock(VideoStorageService.class)
        );

        User usuario = new User();
        usuario.setId(7L);
        Publicacion publicacion = new Publicacion();
        publicacion.setId(701L);
        publicacion.setUser(usuario);

        when(publicaciones.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(publicacion)));
        when(likes.idsLikeadosPorUsuario(7L, Set.of(701L)))
                .thenReturn(Set.of());

        List<PublicacionResponse> resultado = servicio.feed(usuario, 70, 10, "recientes");

        assertThat(resultado).extracting(PublicacionResponse::getId).containsExactly(701L);
        ArgumentCaptor<Pageable> paginacion = ArgumentCaptor.forClass(Pageable.class);
        verify(publicaciones).findAllByOrderByCreatedAtDescIdDesc(paginacion.capture());
        assertThat(paginacion.getValue().getPageNumber()).isEqualTo(70);
        assertThat(paginacion.getValue().getPageSize()).isEqualTo(10);
        verify(publicaciones, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        verifyNoInteractions(amistades);
    }

    @Test
    void paraVosOrdenaPorPopularidadConConteosAgrupados() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        ComentarioRepository comentarios = mock(ComentarioRepository.class);
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

        Publicacion popular = new Publicacion();
        popular.setId(101L);
        popular.setUser(usuario);
        popular.setCreatedAt(LocalDateTime.of(2026, 10, 24, 10, 0));

        Publicacion sinInteracciones = new Publicacion();
        sinInteracciones.setId(202L);
        sinInteracciones.setUser(usuario);
        sinInteracciones.setCreatedAt(LocalDateTime.of(2026, 10, 24, 10, 0));

        Set<Long> ids = Set.of(101L, 202L);
        when(publicaciones.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sinInteracciones, popular)));
        when(amistades.findRelaciones(7L, EstadoAmistad.ACEPTADA))
                .thenReturn(List.of());
        when(likes.autoresPreferidos(7L)).thenReturn(Set.of());
        when(comentarios.autoresComentados(7L)).thenReturn(Set.of());
        when(likes.contarPorPublicaciones(ids))
                .thenReturn(List.<Object[]>of(new Object[]{101L, 10L}));
        when(comentarios.contarPorPublicaciones(ids))
                .thenReturn(List.<Object[]>of(new Object[]{101L, 5L}));
        when(likes.idsLikeadosPorUsuario(7L, ids)).thenReturn(Set.of());

        List<PublicacionResponse> resultado = servicio.feed(usuario, 0, 10, "para-vos");

        assertThat(resultado).extracting(PublicacionResponse::getId)
                .containsExactly(101L, 202L);
        verify(likes, times(1)).contarPorPublicaciones(ids);
        verify(comentarios, times(1)).contarPorPublicaciones(ids);
        verify(likes, times(1)).idsLikeadosPorUsuario(7L, ids);
    }

    @Test
    void paraVosSinCandidatasNoEjecutaConteosAgrupados() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        ComentarioRepository comentarios = mock(ComentarioRepository.class);
        PublicacionService servicio = new PublicacionService(
                publicaciones, comentarios, likes,
                mock(UserRepository.class), mock(AmistadService.class),
                mock(AmistadRepository.class), mock(NotificacionService.class),
                mock(VideoStorageService.class)
        );

        User usuario = new User();
        usuario.setId(7L);
        when(publicaciones.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(servicio.feed(usuario, 0, 10, "para-vos")).isEmpty();
        verify(likes, never()).contarPorPublicaciones(any());
        verify(comentarios, never()).contarPorPublicaciones(any());
    }

    @Test
    void amigosPaginaDesdeMySQLConPublicacionesDeAmigosYPropias() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        AmistadRepository amistades = mock(AmistadRepository.class);
        PublicacionService servicio = new PublicacionService(
                publicaciones, mock(ComentarioRepository.class), likes,
                mock(UserRepository.class), mock(AmistadService.class),
                amistades, mock(NotificacionService.class),
                mock(VideoStorageService.class)
        );

        User usuario = new User();
        usuario.setId(7L);
        User amigo = new User();
        amigo.setId(8L);
        Amistad relacion = new Amistad();
        relacion.setSolicitante(amigo);
        relacion.setDestinatario(usuario);
        when(amistades.findRelaciones(7L, EstadoAmistad.ACEPTADA))
                .thenReturn(List.of(relacion));

        Publicacion antigua = new Publicacion();
        antigua.setId(801L);
        antigua.setUser(amigo);
        antigua.setCreatedAt(LocalDateTime.of(2026, 10, 23, 10, 0));
        when(publicaciones.findFeedAmigos(eq(Set.of(7L, 8L)), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(antigua)));
        when(likes.idsLikeadosPorUsuario(7L, Set.of(801L)))
                .thenReturn(Set.of());

        List<PublicacionResponse> resultado = servicio.feed(usuario, 60, 10, "amigos");

        assertThat(resultado).extracting(PublicacionResponse::getId)
                .containsExactly(801L);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(publicaciones).findFeedAmigos(eq(Set.of(7L, 8L)), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(60);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(10);
        verify(publicaciones, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
        verify(likes, times(1)).idsLikeadosPorUsuario(7L, Set.of(801L));
    }

    @Test
    void amigosSinAmistadesAceptadasIncluyeMisPublicaciones() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        AmistadRepository amistades = mock(AmistadRepository.class);
        PublicacionService servicio = new PublicacionService(
                publicaciones, mock(ComentarioRepository.class), likes,
                mock(UserRepository.class), mock(AmistadService.class),
                amistades, mock(NotificacionService.class),
                mock(VideoStorageService.class)
        );

        User usuario = new User();
        usuario.setId(7L);
        Publicacion propia = new Publicacion();
        propia.setId(77L);
        propia.setUser(usuario);
        when(amistades.findRelaciones(7L, EstadoAmistad.ACEPTADA))
                .thenReturn(List.of());
        when(publicaciones.findFeedAmigos(eq(Set.of(7L)), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(propia)));
        when(likes.idsLikeadosPorUsuario(7L, Set.of(77L)))
                .thenReturn(Set.of());

        assertThat(servicio.feed(usuario, 0, 10, "amigos"))
                .extracting(PublicacionResponse::getId)
                .containsExactly(77L);
        verify(publicaciones).findFeedAmigos(eq(Set.of(7L)), any(Pageable.class));
    }

    @Test
    void amigosPaginaVaciaNoConsultaLikes() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        AmistadRepository amistades = mock(AmistadRepository.class);
        PublicacionService servicio = new PublicacionService(
                publicaciones, mock(ComentarioRepository.class), likes,
                mock(UserRepository.class), mock(AmistadService.class),
                amistades, mock(NotificacionService.class),
                mock(VideoStorageService.class)
        );

        User usuario = new User();
        usuario.setId(7L);
        when(amistades.findRelaciones(7L, EstadoAmistad.ACEPTADA))
                .thenReturn(List.of());
        when(publicaciones.findFeedAmigos(eq(Set.of(7L)), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of()));

        assertThat(servicio.feed(usuario, 5, 10, "amigos")).isEmpty();
        verifyNoInteractions(likes);
        verify(publicaciones, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void paginaCronologicaVaciaNoConsultaLikes() {
        PublicacionRepository publicaciones = mock(PublicacionRepository.class);
        LikeRepository likes = mock(LikeRepository.class);
        PublicacionService servicio = new PublicacionService(
                publicaciones, mock(ComentarioRepository.class), likes,
                mock(UserRepository.class), mock(AmistadService.class),
                mock(AmistadRepository.class), mock(NotificacionService.class),
                mock(VideoStorageService.class)
        );

        User usuario = new User();
        usuario.setId(7L);
        when(publicaciones.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of()));

        assertThat(servicio.feed(usuario, 2, 10, "recientes")).isEmpty();
        verifyNoInteractions(likes);
    }
}
