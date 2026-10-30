package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.Publicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.repository.PublicidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicidadAdminService10ATest {
    private PublicidadRepository repository;
    private BitacoraService bitacora;
    private PublicidadAdminService service;

    @BeforeEach
    void preparar() {
        repository = mock(PublicidadRepository.class);
        bitacora = mock(BitacoraService.class);
        service = new PublicidadAdminService(repository, bitacora);
        when(repository.saveAndFlush(any(Publicidad.class))).thenAnswer(inv -> {
            Publicidad p = inv.getArgument(0);
            if (p.getId() == null) p.setId(7L);
            return p;
        });
    }

    private PublicidadAdminService.Solicitud solicitud() {
        return new PublicidadAdminService.Solicitud(
                "Torneo de verano", "Inscripciones abiertas", "https://ejemplo.com/imagen.jpg",
                "https://ejemplo.com/inscripcion", UbicacionPublicidad.FEED, 5, null, null);
    }

    private Publicidad existente() {
        Publicidad p = new Publicidad();
        p.setId(7L);
        p.setTitulo("Torneo de verano");
        p.setDestinoUrl("https://ejemplo.com/inscripcion");
        p.setUbicacion(UbicacionPublicidad.FEED);
        p.setEstado(EstadoPublicidad.BORRADOR);
        p.setPrioridad(5);
        p.setCreadoEn(Instant.now());
        p.setActualizadoEn(Instant.now());
        p.setCreadoPorId(1L);
        p.setActualizadoPorId(1L);
        return p;
    }

    @Test void crearEnBorradorYAuditar() {
        var respuesta = service.crear(solicitud(), 1L);
        assertEquals(EstadoPublicidad.BORRADOR, respuesta.estado());
        assertEquals(7L, respuesta.id());
        verify(bitacora).evento("PUBLICIDAD_CREADA", "PUBLICIDAD", 7L, 1L);
    }

    @Test void impideDestinoHttp() {
        var s = solicitud();
        var noHttps = new PublicidadAdminService.Solicitud(s.titulo(), s.descripcion(), s.imagenUrl(),
                "http://ejemplo.com", s.ubicacion(), s.prioridad(), null, null);
        assertThrows(ApiException.class, () -> service.crear(noHttps, 1L));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void impideJavascriptComoDestino() {
        var s = solicitud();
        var peligro = new PublicidadAdminService.Solicitud(s.titulo(), s.descripcion(), s.imagenUrl(),
                "javascript:alert(1)", s.ubicacion(), s.prioridad(), null, null);
        assertThrows(ApiException.class, () -> service.crear(peligro, 1L));
    }

    @Test void impidePrioridadFueraDeRango() {
        var s = solicitud();
        var invalida = new PublicidadAdminService.Solicitud(s.titulo(), s.descripcion(), s.imagenUrl(),
                s.destinoUrl(), s.ubicacion(), -1, null, null);
        assertThrows(ApiException.class, () -> service.crear(invalida, 1L));
    }

    @Test void impideRangoFechasInvertido() {
        var s = solicitud();
        var invalida = new PublicidadAdminService.Solicitud(s.titulo(), s.descripcion(), s.imagenUrl(),
                s.destinoUrl(), s.ubicacion(), 0, Instant.parse("2026-11-20T00:00:00Z"),
                Instant.parse("2026-11-10T00:00:00Z"));
        assertThrows(ApiException.class, () -> service.crear(invalida, 1L));
    }

    @Test void requiereTitulo() {
        var s = solicitud();
        var invalida = new PublicidadAdminService.Solicitud("  ", s.descripcion(), s.imagenUrl(),
                s.destinoUrl(), s.ubicacion(), 0, null, null);
        assertThrows(ApiException.class, () -> service.crear(invalida, 1L));
    }

    @Test void activarRegistraAuditoria() {
        when(repository.findById(7L)).thenReturn(Optional.of(existente()));
        var r = service.cambiarEstado(7L, new PublicidadAdminService.CambioEstado(EstadoPublicidad.ACTIVA), 1L);
        assertEquals(EstadoPublicidad.ACTIVA, r.estado());
        verify(bitacora).evento("PUBLICIDAD_ACTIVA", "PUBLICIDAD", 7L, 1L);
    }

    @Test void cambioAlMismoEstadoNoGeneraEvento() {
        when(repository.findById(7L)).thenReturn(Optional.of(existente()));
        service.cambiarEstado(7L, new PublicidadAdminService.CambioEstado(EstadoPublicidad.BORRADOR), 1L);
        verifyNoInteractions(bitacora);
    }

    @Test void noSePuedeReactivarArchivada() {
        Publicidad p = existente();
        p.setEstado(EstadoPublicidad.ARCHIVADA);
        when(repository.findById(7L)).thenReturn(Optional.of(p));
        assertThrows(ApiException.class, () -> service.cambiarEstado(7L,
                new PublicidadAdminService.CambioEstado(EstadoPublicidad.ACTIVA), 1L));
    }

    @Test void publicidadInexistenteEs404() {
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class, () -> service.obtener(5L)).getStatus());
    }

    @Test void rechazaPaginasNegativas() {
        assertThrows(ApiException.class, () -> service.listar("", null, null, -1));
    }

    @Test void edicionAuditaYConservaEstado() {
        Publicidad p = existente();
        p.setEstado(EstadoPublicidad.ACTIVA);
        when(repository.findById(7L)).thenReturn(Optional.of(p));
        var resultado = service.editar(7L, solicitud(), 2L);
        assertEquals(EstadoPublicidad.ACTIVA, resultado.estado());
        assertEquals(2L, resultado.actualizadoPorId());
        verify(bitacora).evento("PUBLICIDAD_EDITADA", "PUBLICIDAD", 7L, 2L);
    }
}
