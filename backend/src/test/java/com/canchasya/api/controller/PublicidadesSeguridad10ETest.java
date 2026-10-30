package com.canchasya.api.controller;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.Publicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.repository.PublicidadRepository;
import com.canchasya.api.service.BitacoraService;
import com.canchasya.api.service.PublicidadAdminService;
import com.canchasya.api.service.PublicidadMetricasService;
import com.canchasya.api.service.PublicidadPublicaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import java.time.Instant;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Etapa 10E - pruebas sin Spring context ni conexión MySQL.
 * Las pruebas de status HTTP real (401/403/200) están en PROBAR_PUBLICIDADES_10E.ps1.
 */
class PublicidadesSeguridad10ETest {
    private PublicidadRepository repositorio;
    private BitacoraService bitacora;
    private NamedParameterJdbcTemplate jdbc;
    private PublicidadAdminService admin;
    private PublicidadMetricasService metricas;

    @BeforeEach
    void preparar() {
        repositorio = mock(PublicidadRepository.class);
        bitacora = mock(BitacoraService.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);
        admin = new PublicidadAdminService(repositorio, bitacora);
        metricas = new PublicidadMetricasService(repositorio, jdbc);
    }

    private Publicidad anuncio(EstadoPublicidad estado) {
        var p = new Publicidad();
        p.setId(19L);
        p.setTitulo("Partido local");
        p.setDestinoUrl("https://example.com/torneo");
        p.setUbicacion(UbicacionPublicidad.FEED);
        p.setEstado(estado);
        p.setPrioridad(3);
        p.setCreadoPorId(1L);
        p.setActualizadoPorId(1L);
        p.setCreadoEn(Instant.now());
        p.setActualizadoEn(Instant.now());
        return p;
    }

    private PublicidadAdminService.Solicitud solicitud(String destino) {
        return new PublicidadAdminService.Solicitud("Torneo prueba", "Prueba local", null,
                destino, UbicacionPublicidad.FEED, 1, null, null);
    }

    private void rechazoRegistro(EstadoPublicidad estado) {
        when(repositorio.findById(19L)).thenReturn(Optional.of(anuncio(estado)));
        var e = assertThrows(ApiException.class,
                () -> metricas.registrar(19L, UbicacionPublicidad.FEED, false));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        verifyNoInteractions(jdbc);
    }

    @Test void administradorRequiereRolEnTodoElControlador() {
        var anotacion = PublicidadAdminController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion);
        assertEquals("hasRole('ADMINISTRADOR')", anotacion.value());
    }

    @Test void rutaAdministrativaNoSeConfundeConLaPublica() {
        var ruta = PublicidadAdminController.class.getAnnotation(RequestMapping.class);
        assertNotNull(ruta);
        assertArrayEquals(new String[]{"/api/admin/publicidades"}, ruta.value());
    }

    @Test void estadisticasRequierenAdministrador() throws Exception {
        var metodo = PublicidadMetricasController.class.getMethod("estadisticas", int.class,
                UbicacionPublicidad.class, Long.class);
        var anotacion = metodo.getAnnotation(PreAuthorize.class);
        assertNotNull(anotacion);
        assertEquals("hasRole('ADMINISTRADOR')", anotacion.value());
        assertEquals("/api/admin/publicidades/estadisticas", metodo.getAnnotation(GetMapping.class).value()[0]);
    }

    @Test void actualizacionDelEstadoNoEsRutaPublica() throws Exception {
        var metodo = PublicidadAdminController.class.getMethod("estado", Long.class,
                PublicidadAdminService.CambioEstado.class,
                org.springframework.security.core.Authentication.class);
        assertEquals("/{id}/estado", metodo.getAnnotation(PatchMapping.class).value()[0]);
        assertNotNull(PublicidadAdminController.class.getAnnotation(PreAuthorize.class));
    }

    @Test void consultasPublicasSoloExponenCamposDelAnuncio() throws Exception {
        Set<String> campos = Arrays.stream(PublicidadPublicaService.Anuncio.class.getRecordComponents())
                .map(RecordComponent::getName).collect(Collectors.toSet());
        assertEquals(Set.of("id", "titulo", "descripcion", "imagenUrl", "destinoUrl", "ubicacion"), campos);
    }

    @Test void laConsultaPublicaTieneRutaEspecifica() throws Exception {
        var clase = PublicidadPublicaController.class.getAnnotation(RequestMapping.class);
        var metodo = PublicidadPublicaController.class.getMethod("visibles", UbicacionPublicidad.class);
        assertEquals("/api/publicidades", clase.value()[0]);
        assertEquals("/visibles", metodo.getAnnotation(GetMapping.class).value()[0]);
    }

    @Test void medicionesNoSonRutasDeAdministracion() throws Exception {
        var impresion = PublicidadMetricasController.class.getMethod("impresion", Long.class,
                PublicidadMetricasController.Registro.class);
        var clic = PublicidadMetricasController.class.getMethod("clic", Long.class,
                PublicidadMetricasController.Registro.class);
        assertEquals("/api/publicidades/{id}/impresiones", impresion.getAnnotation(PostMapping.class).value()[0]);
        assertEquals("/api/publicidades/{id}/clics", clic.getAnnotation(PostMapping.class).value()[0]);
    }

    @Test void respuestaListadoAdminEvitaCache() {
        var servicio = mock(PublicidadAdminService.class);
        when(servicio.listar(anyString(), isNull(), isNull(), eq(0)))
                .thenReturn(new PageImpl<>(List.of()));
        var respuesta = new PublicidadAdminController(servicio).listar("", null, null, 0);
        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getHeaders().getCacheControl().contains("no-store"));
    }

    @Test void respuestaListaPublicaEvitaCache() {
        var servicio = mock(PublicidadPublicaService.class);
        when(servicio.visibles(UbicacionPublicidad.FEED)).thenReturn(List.of());
        var respuesta = new PublicidadPublicaController(servicio).visibles(UbicacionPublicidad.FEED);
        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getHeaders().getCacheControl().contains("no-store"));
    }

    @Test void impresionResponde204YNoCache() {
        var servicio = mock(PublicidadMetricasService.class);
        var respuesta = new PublicidadMetricasController(servicio).impresion(19L,
                new PublicidadMetricasController.Registro(UbicacionPublicidad.FEED));
        assertEquals(204, respuesta.getStatusCode().value());
        assertTrue(respuesta.getHeaders().getCacheControl().contains("no-store"));
        verify(servicio).registrar(19L, UbicacionPublicidad.FEED, false);
    }

    @Test void clicResponde204YNoCache() {
        var servicio = mock(PublicidadMetricasService.class);
        var respuesta = new PublicidadMetricasController(servicio).clic(19L,
                new PublicidadMetricasController.Registro(UbicacionPublicidad.FEED));
        assertEquals(204, respuesta.getStatusCode().value());
        verify(servicio).registrar(19L, UbicacionPublicidad.FEED, true);
    }

    @Test void registroSinUbicacionEs400YNoAccedeAMysql() {
        var e = assertThrows(ApiException.class, () -> metricas.registrar(19L, null, true));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verifyNoInteractions(repositorio, jdbc);
    }

    @Test void registroConIdCeroEs400YNoAccedeAMysql() {
        var e = assertThrows(ApiException.class,
                () -> metricas.registrar(0L, UbicacionPublicidad.FEED, false));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verifyNoInteractions(repositorio, jdbc);
    }

    @Test void noRegistraImpresionEnBorrador() { rechazoRegistro(EstadoPublicidad.BORRADOR); }
    @Test void noRegistraImpresionEnPausada() { rechazoRegistro(EstadoPublicidad.PAUSADA); }
    @Test void noRegistraImpresionEnArchivada() { rechazoRegistro(EstadoPublicidad.ARCHIVADA); }

    @Test void noRegistraImpresionVencida() {
        var p = anuncio(EstadoPublicidad.ACTIVA);
        p.setFechaFin(Instant.now().minusSeconds(60));
        when(repositorio.findById(19L)).thenReturn(Optional.of(p));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class, () -> metricas.registrar(19L, UbicacionPublicidad.FEED, false))
                        .getStatus());
        verifyNoInteractions(jdbc);
    }

    @Test void noRegistraImpresionProgramadaParaElFuturo() {
        var p = anuncio(EstadoPublicidad.ACTIVA);
        p.setFechaInicio(Instant.now().plusSeconds(3600));
        when(repositorio.findById(19L)).thenReturn(Optional.of(p));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class, () -> metricas.registrar(19L, UbicacionPublicidad.FEED, false))
                        .getStatus());
        verifyNoInteractions(jdbc);
    }

    @Test void noRegistraClicEnOtraUbicacion() {
        when(repositorio.findById(19L)).thenReturn(Optional.of(anuncio(EstadoPublicidad.ACTIVA)));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ApiException.class,
                        () -> metricas.registrar(19L, UbicacionPublicidad.INICIO, true)).getStatus());
        verifyNoInteractions(jdbc);
    }

    @Test void estadisticasRechazanPeriodosDistintosDe7y30() {
        for (int dias : new int[] {-1, 0, 1, 8, 31, 365}) {
            assertEquals(HttpStatus.BAD_REQUEST,
                    assertThrows(ApiException.class,
                            () -> metricas.estadisticas(dias, null, null)).getStatus());
        }
        verifyNoInteractions(repositorio, jdbc);
    }

    @Test void estadisticasRechazanPublicidadConIdNegativo() {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ApiException.class,
                        () -> metricas.estadisticas(7, null, -5L)).getStatus());
        verifyNoInteractions(repositorio, jdbc);
    }

    @Test void crearPublicidadRechazaUrlsNoSeguras() {
        for (String url : List.of("http://example.com", "javascript:alert(1)",
                "https://usuario:clave@example.com/", "https://example.com:8080/",
                "ftp://example.com")) {
            assertEquals(HttpStatus.BAD_REQUEST,
                    assertThrows(ApiException.class,
                            () -> admin.crear(solicitud(url), 1L)).getStatus(), url);
        }
        verifyNoInteractions(repositorio, bitacora);
    }

    @Test void editarPublicidadArchivadaEs409SinAuditoria() {
        when(repositorio.findById(19L)).thenReturn(Optional.of(anuncio(EstadoPublicidad.ARCHIVADA)));
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApiException.class,
                        () -> admin.editar(19L, solicitud("https://example.com/"), 1L)).getStatus());
        verify(repositorio, never()).saveAndFlush(any(Publicidad.class));
        verifyNoInteractions(bitacora);
    }

    @Test void noReactivarPublicidadArchivadaEs409SinAuditoria() {
        when(repositorio.findById(19L)).thenReturn(Optional.of(anuncio(EstadoPublicidad.ARCHIVADA)));
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ApiException.class,
                        () -> admin.cambiarEstado(19L,
                                new PublicidadAdminService.CambioEstado(EstadoPublicidad.ACTIVA), 1L)).getStatus());
        verify(repositorio, never()).saveAndFlush(any(Publicidad.class));
        verifyNoInteractions(bitacora);
    }
}
