package com.canchasya.api.controller;

import com.canchasya.api.model.BitacoraEvento;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.BitacoraRepository;
import com.canchasya.api.service.BitacoraService;
import com.canchasya.api.service.FrontendReportLimiter;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 09E5: pruebas unitarias rápidas, sin MySQL ni credenciales.
 * La anotación PreAuthorize se revisa aquí; la autorización HTTP real (401/403)
 * se comprueba con PROBAR_BITACORA_09E5.ps1 o en una sesión local de pruebas.
 */
@ExtendWith(MockitoExtension.class)
class BitacoraController09E5Test {

    @Mock private BitacoraRepository repository;
    @Mock private BitacoraService bitacora;
    @Mock private FrontendReportLimiter frontendReportLimiter;

    private BitacoraController controller;

    @BeforeEach
    void preparar() {
        controller = new BitacoraController(repository, bitacora, frontendReportLimiter);
    }

    private static void esperarEstado(int codigo, org.junit.jupiter.api.function.Executable operacion) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, operacion);
        assertEquals(codigo, ex.getStatusCode().value());
    }

    private static void requiereRolAdmin(String metodo) {
        Method encontrado = null;
        for (Method m : BitacoraController.class.getDeclaredMethods()) {
            if (m.getName().equals(metodo)) {
                encontrado = m;
                break;
            }
        }
        assertNotNull(encontrado, "Falta endpoint " + metodo);
        PreAuthorize autorizacion = encontrado.getAnnotation(PreAuthorize.class);
        assertNotNull(autorizacion, "Falta @PreAuthorize en " + metodo);
        assertEquals("hasRole('ADMINISTRADOR')", autorizacion.value(),
                "No se debe habilitar el endpoint a otros roles: " + metodo);
    }

    @Test
    void todosLosEndpointsDeAdministracionExigenRol() {
        for (String nombre : List.of("listar", "estadisticas", "listarAcciones",
                "listarIncidentes", "historialIncidente", "cambiarEstado")) {
            requiereRolAdmin(nombre);
        }
        Method diagnostico = null;
        for (Method m : BitacoraController.class.getDeclaredMethods()) {
            if (m.getName().equals("errorFrontend")) diagnostico = m;
        }
        assertNotNull(diagnostico);
        assertEquals("isAuthenticated()", diagnostico.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void elListadoRechazaPaginasFiltrosYBusquedasInvalidas() {
        esperarEstado(400, () -> controller.listar(null, null, null, null, null, -1));
        esperarEstado(400, () -> controller.listar(null, null, null, null, null, 10001));
        esperarEstado(400, () -> controller.listar("DEBUG", null, null, null, null, 0));
        esperarEstado(400, () -> controller.listar(null, "ADMIN", null, null, null, 0));
        esperarEstado(400, () -> controller.listar(null, null, "testing", null, null, 0));
        esperarEstado(400, () -> controller.listar(null, null, null, "CERRADO", null, 0));
        esperarEstado(400, () -> controller.listar(null, null, null, null, "x".repeat(101), 0));
        esperarEstado(400, () -> controller.listar(null, null, null, null, "busqueda\nmaliciosa", 0));
        verifyNoInteractions(repository);
    }

    @Test
    void elHistorialDeAccionesRechazaFiltrosInvalidos() {
        esperarEstado(400, () -> controller.listarAcciones(-1, null, null, null));
        esperarEstado(400, () -> controller.listarAcciones(10001, null, null, null));
        esperarEstado(400, () -> controller.listarAcciones(0, "TIPO_DESCONOCIDO", null, null));
        esperarEstado(400, () -> controller.listarAcciones(0, null, 0L, null));
        esperarEstado(400, () -> controller.listarAcciones(0, null, -1L, null));
        esperarEstado(400, () -> controller.listarAcciones(0, null, null, "x".repeat(101)));
        verifyNoInteractions(repository);
    }

    @Test
    void laColaDeIncidentesRechazaFiltrosInvalidos() {
        esperarEstado(400, () -> controller.listarIncidentes(-1, null, null, null, null, false, null));
        esperarEstado(400, () -> controller.listarIncidentes(10001, null, null, null, null, false, null));
        esperarEstado(400, () -> controller.listarIncidentes(0, "DESCARTADO", null, null, null, false, null));
        esperarEstado(400, () -> controller.listarIncidentes(0, null, "MOVIL", null, null, false, null));
        esperarEstado(400, () -> controller.listarIncidentes(0, null, null, "dev", null, false, null));
        esperarEstado(400, () -> controller.listarIncidentes(0, null, null, null, 0L, false, null));
        esperarEstado(400, () -> controller.listarIncidentes(0, null, null, null, 1L, true, null));
        esperarEstado(400, () -> controller.listarIncidentes(0, null, null, null, null, false, "x".repeat(101)));
        verifyNoInteractions(repository);
    }

    @Test
    void estadisticasPermiteSoloPeriodosYAmbientesSoportados() {
        esperarEstado(400, () -> controller.estadisticas(0, null));
        esperarEstado(400, () -> controller.estadisticas(8, null));
        esperarEstado(400, () -> controller.estadisticas(31, null));
        esperarEstado(400, () -> controller.estadisticas(7, "staging"));
        verifyNoInteractions(repository);
    }

    @Test
    void estadisticasDevuelveSieteBarrasYNoAlmacenaCache() {
        var respuesta = controller.estadisticas(7, "local");
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(7, respuesta.getBody().dias());
        assertEquals("local", respuesta.getBody().ambiente());
        assertEquals("UTC", respuesta.getBody().zonaHoraria());
        assertEquals(7, respuesta.getBody().tendencia().size());
        assertEquals(0L, respuesta.getBody().totalErrores());
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
    }

    @Test
    void estadisticasDeTreintaDiasDevuelveSeisTramos() {
        var respuesta = controller.estadisticas(30, null);
        assertNotNull(respuesta.getBody());
        assertEquals("todos", respuesta.getBody().ambiente());
        assertEquals(6, respuesta.getBody().tendencia().size());
    }

    @Test
    void cadaListadoAplicaPaginacionLimitadaYSinCache() {
        Page<BitacoraEvento> vacia = new PageImpl<>(List.of());
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(vacia);

        var general = controller.listar(null, null, null, null, null, 0);
        var acciones = controller.listarAcciones(0, null, null, null);
        var incidentes = controller.listarIncidentes(0, null, null, null, null, false, null);

        assertEquals("no-store", general.getHeaders().getCacheControl());
        assertEquals("no-store", acciones.getHeaders().getCacheControl());
        assertEquals("no-store", incidentes.getHeaders().getCacheControl());

        ArgumentCaptor<Pageable> paginaciones = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(3)).findAll(any(Specification.class), paginaciones.capture());
        assertEquals(25, paginaciones.getAllValues().get(0).getPageSize());
        assertEquals(15, paginaciones.getAllValues().get(1).getPageSize());
        assertEquals(15, paginaciones.getAllValues().get(2).getPageSize());
    }

    @Test
    void historialExigeIdYTipoDeIncidenteCorrectos() {
        esperarEstado(400, () -> controller.historialIncidente(0L, 0));
        esperarEstado(400, () -> controller.historialIncidente(1L, -1));
        esperarEstado(400, () -> controller.historialIncidente(1L, 10001));
        esperarEstado(404, () -> controller.historialIncidente(999L, 0));

        BitacoraEvento informacion = new BitacoraEvento();
        informacion.setNivel("INFO");
        when(repository.findById(8L)).thenReturn(java.util.Optional.of(informacion));
        esperarEstado(409, () -> controller.historialIncidente(8L, 0));
    }

    @Test
    void historialDeIncidenteRealEsSoloLectura() {
        BitacoraEvento incidente = new BitacoraEvento();
        incidente.setNivel("ERROR");
        when(repository.findById(8L)).thenReturn(java.util.Optional.of(incidente));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var respuesta = controller.historialIncidente(8L, 0);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    void cambioDeEstadoNoPuedeEditarEventosInformativosNiIdsInexistentes() {
        User admin = mock(User.class);
        esperarEstado(404, () -> controller.cambiarEstado(999L,
                new BitacoraController.CambiarEstadoRequest("RESUELTO"), admin));
        BitacoraEvento info = new BitacoraEvento();
        info.setNivel("INFO");
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(info));
        esperarEstado(409, () -> controller.cambiarEstado(7L,
                new BitacoraController.CambiarEstadoRequest("RESUELTO"), admin));
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(bitacora);
    }

    @Test
    void cambioDeEstadoRechazaValoresDesconocidos() {
        User admin = mock(User.class);
        BitacoraEvento error = new BitacoraEvento();
        error.setNivel("ERROR");
        error.setEstado("PENDIENTE");
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(error));
        esperarEstado(400, () -> controller.cambiarEstado(7L,
                new BitacoraController.CambiarEstadoRequest("IGNORADO"), admin));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void cambioRealActualizaResponsableYAuditaLaGestion() {
        User admin = mock(User.class);
        when(admin.getId()).thenReturn(42L);
        BitacoraEvento error = new BitacoraEvento();
        error.setNivel("ERROR");
        error.setEstado("PENDIENTE");
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(error));
        when(repository.saveAndFlush(error)).thenReturn(error);

        var respuesta = controller.cambiarEstado(7L,
                new BitacoraController.CambiarEstadoRequest("RESUELTO"), admin);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals("RESUELTO", respuesta.getBody().estado());
        assertEquals(42L, respuesta.getBody().atendidoPorId());
        assertNotNull(respuesta.getBody().atendidoEn());
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        verify(repository).saveAndFlush(error);
        verify(bitacora).evento("INCIDENTE_ESTADO_CAMBIADO_ADMIN", "INCIDENTE", 7L, 42L);
    }

    @Test
    void repetirElMismoEstadoNoCreaAuditoriaFalsa() {
        User admin = mock(User.class);
        when(admin.getId()).thenReturn(42L);
        BitacoraEvento error = new BitacoraEvento();
        error.setNivel("ERROR");
        error.setEstado("RESUELTO");
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(error));
        when(repository.saveAndFlush(error)).thenReturn(error);

        controller.cambiarEstado(7L, new BitacoraController.CambiarEstadoRequest("RESUELTO"), admin);
        verify(repository).saveAndFlush(error);
        verifyNoInteractions(bitacora);
    }

    @Test
    void laValidacionDelBodyLimitaElTamanoDelEstado() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(
                    new BitacoraController.CambiarEstadoRequest("x".repeat(21))).isEmpty());
            assertFalse(validator.validate(
                    new BitacoraController.CambiarEstadoRequest("  ")).isEmpty());
            assertTrue(validator.validate(
                    new BitacoraController.CambiarEstadoRequest("EN_INVESTIGACION")).isEmpty());
        }
    }

    @Test
    void elReporteFrontendRequiereIdentidadYAplicaLimitador() {
        User usuario = mock(User.class);
        when(usuario.getId()).thenReturn(10L);
        when(bitacora.errorFrontend(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("SF-CODIGO-PRUEBA");
        var entrada = new BitacoraController.FrontendErrorRequest("Error", "/inicio", null, null, null, null);
        var respuesta = controller.errorFrontend(entrada, usuario);
        assertEquals(HttpStatus.ACCEPTED, respuesta.getStatusCode());
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        verify(frontendReportLimiter).permitir(10L);
        assertEquals("SF-CODIGO-PRUEBA", respuesta.getBody().codigo());
    }
}
