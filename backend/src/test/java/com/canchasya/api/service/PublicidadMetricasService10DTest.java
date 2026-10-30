package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.Publicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.repository.PublicidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicidadMetricasService10DTest {
    private PublicidadRepository repository;
    private NamedParameterJdbcTemplate jdbc;
    private PublicidadMetricasService metricas;

    @BeforeEach
    void iniciar() {
        repository = mock(PublicidadRepository.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);
        metricas = new PublicidadMetricasService(repository, jdbc);
    }

    private Publicidad anuncio() {
        Publicidad p = new Publicidad();
        p.setId(12L);
        p.setUbicacion(UbicacionPublicidad.FEED);
        p.setEstado(EstadoPublicidad.ACTIVA);
        return p;
    }

    @Test
    void contabilizaImpresionDeAnuncioActivo() {
        when(repository.findById(12L)).thenReturn(Optional.of(anuncio()));
        metricas.registrar(12L, UbicacionPublicidad.FEED, false);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(SqlParameterSource.class));
        assertTrue(sql.getValue().contains("impresiones = impresiones + 1"));
        assertFalse(sql.getValue().contains("clics = clics + 1"));
    }

    @Test
    void contabilizaClicDeAnuncioActivo() {
        when(repository.findById(12L)).thenReturn(Optional.of(anuncio()));
        metricas.registrar(12L, UbicacionPublicidad.FEED, true);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(SqlParameterSource.class));
        assertTrue(sql.getValue().contains("clics = clics + 1"));
    }

    @Test
    void evitaContarPublicidadPausada() {
        Publicidad p = anuncio(); p.setEstado(EstadoPublicidad.PAUSADA);
        when(repository.findById(12L)).thenReturn(Optional.of(p));
        assertThrows(ApiException.class, () -> metricas.registrar(12L, UbicacionPublicidad.FEED, false));
        verifyNoInteractions(jdbc);
    }

    @Test
    void evitaContarPublicidadDeOtraUbicacion() {
        when(repository.findById(12L)).thenReturn(Optional.of(anuncio()));
        assertThrows(ApiException.class, () -> metricas.registrar(12L, UbicacionPublicidad.INICIO, false));
        verifyNoInteractions(jdbc);
    }

    @Test
    void evitaContarPublicidadFueraDeFecha() {
        Publicidad p = anuncio(); p.setFechaInicio(Instant.now().plusSeconds(3600));
        when(repository.findById(12L)).thenReturn(Optional.of(p));
        assertThrows(ApiException.class, () -> metricas.registrar(12L, UbicacionPublicidad.FEED, false));
        verifyNoInteractions(jdbc);
    }

    @Test
    void evitaContarPublicidadInexistente() {
        when(repository.findById(17L)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> metricas.registrar(17L, UbicacionPublicidad.FEED, true));
        verifyNoInteractions(jdbc);
    }

    @Test
    void rechazaIdInvalido() {
        assertThrows(ApiException.class, () -> metricas.registrar(0L, UbicacionPublicidad.FEED, true));
        verifyNoInteractions(repository, jdbc);
    }

    @Test
    void rechazaPeriodosNoPermitidos() {
        assertThrows(ApiException.class, () -> metricas.estadisticas(365, null, null));
        assertThrows(ApiException.class, () -> metricas.estadisticas(7, null, -1L));
        verifyNoInteractions(repository, jdbc);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void completaConCerosLosSieteDiasSinDatos() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        var datos = metricas.estadisticas(7, null, null);
        assertEquals(7, datos.evolucion().size());
        assertEquals(0, datos.totales().impresiones());
        assertEquals(0, datos.totales().clics());
        assertEquals(0, datos.totales().ctr());
        assertEquals(LocalDate.now(PublicidadMetricasService.ZONA), datos.hasta());
        assertTrue(datos.anuncios().isEmpty());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void completaConCerosLosTreintaDiasSinDatos() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        var datos = metricas.estadisticas(30, UbicacionPublicidad.FEED, 12L);
        assertEquals(30, datos.evolucion().size());
        assertEquals(30, java.time.temporal.ChronoUnit.DAYS.between(datos.desde(), datos.hasta().plusDays(1)));
    }
}
