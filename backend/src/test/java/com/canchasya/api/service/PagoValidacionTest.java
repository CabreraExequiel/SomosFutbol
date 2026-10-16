package com.canchasya.api.service;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.*;
import com.canchasya.api.config.TimeConfig;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.*;
import org.springframework.http.*;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class PagoValidacionTest {
 PagoRepository pagos;ReservaRepository reservas;CanchaRepository canchas;MercadoPagoCuentaService cuentas;NotificacionService notificaciones;
 Reserva reserva;Pago pago;Clock clock=Clock.fixed(Instant.parse("2026-10-09T15:00:00Z"),TimeConfig.ZONA);
 @BeforeEach void setup(){
  pagos=mock(PagoRepository.class);reservas=mock(ReservaRepository.class);canchas=mock(CanchaRepository.class);cuentas=mock(MercadoPagoCuentaService.class);notificaciones=mock(NotificacionService.class);
  var u=new User();u.setId(1L);u.setName("Jugador");var dueno=new User();dueno.setId(2L);var cancha=new Cancha();cancha.setId(3L);cancha.setNombre("Central");cancha.setDueno(dueno);
  reserva=new Reserva();reserva.setId(4L);reserva.setUser(u);reserva.setCancha(cancha);reserva.setEstado(EstadoReserva.PENDIENTE);reserva.setPagoRequerido(true);reserva.setExpiresAt(clock.instant().plusSeconds(600));
  pago=new Pago();pago.setId(5L);pago.setReserva(reserva);pago.setMonto(new BigDecimal("1500.00"));pago.setMpPreferenceId("pref-5");pago.setEstado(EstadoPago.PENDIENTE);
  when(cuentas.accessToken(2L)).thenReturn("seller-token");when(cuentas.collector(2L)).thenReturn("200");when(pagos.findById(5L)).thenReturn(Optional.of(pago));when(pagos.findByIdForUpdate(5L)).thenReturn(Optional.of(pago));when(canchas.findByIdForUpdate(3L)).thenReturn(Optional.of(cancha));when(reservas.findByIdForUpdate(4L)).thenReturn(Optional.of(reserva));when(pagos.findByMpPaymentId("123")).thenReturn(Optional.empty());
 }
 PagoService service(String estado,String monto,String collector,boolean live,String preference){
  String json="{\"id\":123,\"collector_id\":"+collector+",\"external_reference\":\"5\",\"transaction_amount\":"+monto+",\"currency_id\":\"ARS\",\"preference_id\":\""+preference+"\",\"live_mode\":"+live+",\"metadata\":{\"pago_id\":5,\"reserva_id\":4},\"status\":\""+estado+"\",\"date_approved\":\"2026-10-09T14:59:00Z\"}";
  WebClient client=WebClient.builder().exchangeFunction(req->{assertThat(req.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer seller-token");return Mono.just(ClientResponse.create(HttpStatus.OK).header(HttpHeaders.CONTENT_TYPE,org.springframework.http.MediaType.APPLICATION_JSON_VALUE).body(json).build());}).build();
  var s=new PagoService(pagos,reservas,client,mock(MailService.class),notificaciones,cuentas,clock,canchas);ReflectionTestUtils.setField(s,"testMode",false);return s;
 }
 @Test void confirmaSoloPagoDelVendedorConMontoEntornoYReferenciaValidos(){service("approved","1500","200",true,"pref-5").procesarWebhook("123",2L);assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);}
 @Test void importeAlteradoNoConfirma(){service("approved","1","200",true,"pref-5").procesarWebhook("123",2L);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);}
 @Test void vendedorDiferenteNoConfirma(){service("approved","1500","999",true,"pref-5").procesarWebhook("123",2L);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);verify(pagos,never()).save(any());}
 @Test void pagoDePruebaNoConfirmaProduccion(){service("approved","1500","200",false,"pref-5").procesarWebhook("123",2L);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);}
 @Test void preferenciaIncorrectaNoConfirma(){service("approved","1500","200",true,"pref-otra").procesarWebhook("123",2L);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDIENTE);}
 @Test void unWebhookDeReembolsoActualizaUnPagoYaAprobado(){pago.setEstado(EstadoPago.APROBADO);pago.setMpPaymentId("123");reserva.setEstado(EstadoReserva.CONFIRMADA);service("refunded","1500","200",true,"pref-5").procesarWebhook("123",2L);assertThat(pago.getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);}
 @Test void eventosViejosNoDegradanUnPagoAprobado(){pago.setEstado(EstadoPago.APROBADO);pago.setMpPaymentId("123");reserva.setEstado(EstadoReserva.CONFIRMADA);service("pending","1500","200",true,"pref-5").procesarWebhook("123",2L);assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);}
 @Test void unPagoTardioNoReocupaUnTurnoLiberado(){reserva.setExpiresAt(clock.instant().minusSeconds(1));service("approved","1500","200",true,"pref-5").procesarWebhook("123",2L);assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);verify(notificaciones,atLeastOnce()).crear(any(),any(),eq(TipoNotificacion.PAGO),contains("Reintegro"),anyString(),anyString(),eq(4L));}
}
