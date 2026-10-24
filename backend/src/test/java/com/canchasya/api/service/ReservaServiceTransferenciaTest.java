package com.canchasya.api.service;

import com.canchasya.api.dto.DatosTransferenciaResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Cancha;
import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Role;
import com.canchasya.api.model.Reserva;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.CanchaRepository;
import com.canchasya.api.repository.PagoRepository;
import com.canchasya.api.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservaServiceTransferenciaTest {

    private ReservaRepository reservaRepository;
    private ReservaService service;
    private User jugador;
    private Reserva reserva;

    @BeforeEach
    void configurar() {
        reservaRepository = mock(ReservaRepository.class);
        CanchaRepository canchaRepository = mock(CanchaRepository.class);
        PagoRepository pagoRepository = mock(PagoRepository.class);
        NotificacionService notificacionService = mock(NotificacionService.class);
        service = new ReservaService(reservaRepository, canchaRepository, pagoRepository, notificacionService, mock(DisponibilidadService.class),java.time.Clock.system(com.canchasya.api.config.TimeConfig.ZONA),mock(MercadoPagoCuentaService.class),mock(BitacoraService.class));
        ReflectionTestUtils.setField(service, "paymentsEnabled", false);

        jugador = usuario(1L, "Jugador", Role.USUARIO);
        User dueno = usuario(2L, "Complejo Centro", Role.DUENO);
        dueno.setCbuCvu("1234567890123456789012");
        dueno.setAliasMercadoPago("complejo.centro");

        Cancha cancha = new Cancha();
        cancha.setNombre("Cancha Central");
        cancha.setDueno(dueno);

        reserva = new Reserva();
        reserva.setId(10L);
        reserva.setUser(jugador);
        reserva.setCancha(cancha);
        reserva.setMonto(new BigDecimal("15000.00"));
        reserva.setEstado(EstadoReserva.PENDIENTE);
        when(reservaRepository.findById(10L)).thenReturn(Optional.of(reserva));
    }

    @Test
    void entregaLosDatosSoloParaLaReservaAutorizada() {
        DatosTransferenciaResponse response = service.datosTransferencia(10L, jugador);

        assertThat(response.getReservaId()).isEqualTo(10L);
        assertThat(response.getCbuCvu()).isEqualTo("1234567890123456789012");
        assertThat(response.getAliasMercadoPago()).isEqualTo("complejo.centro");
        assertThat(response.getMonto()).isEqualByComparingTo("15000.00");
    }

    @Test
    void noExponeDatosAUnTercero() {
        User tercero = usuario(3L, "Tercero", Role.USUARIO);

        assertThatThrownBy(() -> service.datosTransferencia(10L, tercero))
                .isInstanceOfSatisfying(ApiException.class, error ->
                        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN)
                );
    }

    @Test
    void noCombinaTransferenciaManualConCheckoutPro() {
        reserva.setPagoRequerido(true);

        assertThatThrownBy(() -> service.datosTransferencia(10L, jugador))
                .isInstanceOfSatisfying(ApiException.class, error ->
                        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT)
                );
    }

    private User usuario(Long id, String nombre, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(nombre);
        user.setRole(role);
        return user;
    }
}
