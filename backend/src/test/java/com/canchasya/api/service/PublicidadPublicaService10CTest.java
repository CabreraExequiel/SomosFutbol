package com.canchasya.api.service;

import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.Publicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.repository.PublicidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicidadPublicaService10CTest {
    private PublicidadRepository repository;
    private PublicidadPublicaService service;

    @BeforeEach
    void configurar() {
        repository = mock(PublicidadRepository.class);
        service = new PublicidadPublicaService(repository);
    }

    private Publicidad ejemplo() {
        Publicidad p = new Publicidad();
        p.setId(23L);
        p.setTitulo("Torneo barrial");
        p.setDescripcion("Inscripciones abiertas");
        p.setDestinoUrl("https://ejemplo.com/torneo");
        p.setImagenUrl("https://ejemplo.com/torneo.jpg");
        p.setUbicacion(UbicacionPublicidad.FEED);
        p.setEstado(EstadoPublicidad.ACTIVA);
        p.setPrioridad(15);
        p.setCreadoPorId(91L);
        p.setActualizadoPorId(92L);
        p.setCreadoEn(Instant.now());
        return p;
    }

    @Test
    void muestraSoloCamposPublicos() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ejemplo())));
        var anuncios = service.visibles(UbicacionPublicidad.FEED);
        assertEquals(1, anuncios.size());
        assertEquals("Torneo barrial", anuncios.get(0).titulo());
        assertEquals("https://ejemplo.com/torneo", anuncios.get(0).destinoUrl());
        assertEquals(6, PublicidadPublicaService.Anuncio.class.getRecordComponents().length);
        assertFalse(java.util.Arrays.stream(PublicidadPublicaService.Anuncio.class.getRecordComponents())
                .anyMatch(c -> c.getName().contains("PorId")));
    }

    @Test
    void consultaLimitaADosYOrdenaPorPrioridad() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.visibles(UbicacionPublicidad.BUSCAR_CANCHAS);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        Pageable pagina = captor.getValue();
        assertEquals(2, pagina.getPageSize());
        assertEquals(0, pagina.getPageNumber());
        assertEquals(org.springframework.data.domain.Sort.Direction.DESC,
                pagina.getSort().getOrderFor("prioridad").getDirection());
    }

    @Test
    void ubicacionObligatoria() {
        assertThrows(IllegalArgumentException.class, () -> service.visibles(null));
        verifyNoInteractions(repository);
    }

    @Test
    void listaVaciaNoEsError() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(service.visibles(UbicacionPublicidad.INICIO).isEmpty());
    }
}
