package com.canchasya.api.service;

import com.canchasya.api.dto.PagoResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Cancha;
import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Pago;
import com.canchasya.api.model.Reserva;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.PagoRepository;
import com.canchasya.api.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PagoServiceTest {

    private PagoRepository pagoRepository;

    private ReservaRepository reservaRepository;

    private PagoService pagoService;

    @BeforeEach
    void setUp() {
        pagoRepository = proxy(PagoRepository.class, (method, args) -> null);
        reservaRepository = proxy(ReservaRepository.class, (method, args) -> null);
        pagoService = new PagoService(
                pagoRepository,
                reservaRepository,
                WebClient.create(),
                null,
                null,
                org.mockito.Mockito.mock(MercadoPagoCuentaService.class),
                java.time.Clock.system(com.canchasya.api.config.TimeConfig.ZONA),
                org.mockito.Mockito.mock(com.canchasya.api.repository.CanchaRepository.class)
        );
        ReflectionTestUtils.setField(pagoService, "frontendUrl", "https://somosfutbol.test");
    }

    @Test
    void reutilizaLaPreferenciaExistenteSinCrearOtroCheckout() {
        User jugador = usuario(1L, "Jugador");
        Reserva reserva = reserva(10L, jugador);
        Pago pago = new Pago();
        pago.setId(20L);
        pago.setReserva(reserva);
        pago.setMonto(new BigDecimal("15000.00"));
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setMpPreferenceId("pref-20");
        pago.setCheckoutUrl("https://www.mercadopago.com.ar/checkout/v1/redirect");

        AtomicReference<Pago> guardado = new AtomicReference<>();
        reservaRepository = proxy(ReservaRepository.class, (method, args) -> switch (method) {
            case "findByIdForUpdate" -> Optional.of(reserva);
            default -> null;
        });
        pagoRepository = proxy(PagoRepository.class, (method, args) -> switch (method) {
            case "findByReservaId" -> Optional.of(pago);
            case "save" -> {
                guardado.set((Pago) args[0]);
                yield args[0];
            }
            default -> null;
        });
        crearServicio();

        PagoResponse response = pagoService.crearPreferencia(10L, jugador);

        assertEquals(EstadoPago.PENDIENTE, response.getEstado());
        assertEquals(pago.getCheckoutUrl(), response.getInitPoint());
        assertSame(pago, guardado.get());
    }

    @Test
    void impideQueOtroUsuarioPagueLaReserva() {
        User jugador = usuario(1L, "Jugador");
        User intruso = usuario(2L, "Otro");
        Reserva reserva = reserva(10L, jugador);
        reservaRepository = proxy(ReservaRepository.class, (method, args) ->
                "findByIdForUpdate".equals(method) ? Optional.of(reserva) : null
        );
        crearServicio();

        ApiException error = assertThrows(
                ApiException.class,
                () -> pagoService.crearPreferencia(10L, intruso)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
    }

    @Test
    void rechazaWebhooksSinFirmaCompleta() {
        ApiException error = assertThrows(
                ApiException.class,
                () -> pagoService.validarFirmaWebhook(null, "request-id", "123")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }

    @Test
    void filtraParametrosNoValidosAlConstruirElRetorno() {
        URI uri = pagoService.construirRetornoFrontend(
                "../../externo",
                "1&admin=true",
                "preferencia con espacios"
        );

        assertEquals("https://somosfutbol.test/reservas?pago=pendiente", uri.toString());
    }

    private User usuario(Long id, String nombre) {
        User user = new User();
        user.setId(id);
        user.setName(nombre);
        user.setEmail(nombre.toLowerCase() + "@example.com");
        return user;
    }

    private Reserva reserva(Long id, User jugador) {
        User dueno = usuario(99L, "Dueno");
        Cancha cancha = new Cancha();
        cancha.setId(30L);
        cancha.setNombre("Cancha Central");
        cancha.setDueno(dueno);

        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setPagoRequerido(true);
        reserva.setUser(jugador);
        reserva.setCancha(cancha);
        reserva.setFecha(LocalDate.now().plusDays(1));
        reserva.setHoraInicio(LocalTime.of(18, 0));
        reserva.setHoraFin(LocalTime.of(19, 0));
        reserva.setMonto(new BigDecimal("15000.00"));
        reserva.setEstado(EstadoReserva.PENDIENTE);
        return reserva;
    }

    private void crearServicio() {
        pagoService = new PagoService(
                pagoRepository,
                reservaRepository,
                WebClient.create(),
                null,
                null,
                org.mockito.Mockito.mock(MercadoPagoCuentaService.class),
                java.time.Clock.system(com.canchasya.api.config.TimeConfig.ZONA),
                org.mockito.Mockito.mock(com.canchasya.api.repository.CanchaRepository.class)
        );
        ReflectionTestUtils.setField(pagoService, "frontendUrl", "https://somosfutbol.test");
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> tipo, ProxyHandler handler) {
        return (T) Proxy.newProxyInstance(
                tipo.getClassLoader(),
                new Class<?>[]{tipo},
                (proxy, method, args) -> handler.invoke(method.getName(), args == null ? new Object[0] : args)
        );
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args);
    }
}
