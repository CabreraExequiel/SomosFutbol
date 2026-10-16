package com.canchasya.api.service;
import com.canchasya.api.config.TimeConfig;
import com.canchasya.api.dto.*;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.*;
import com.canchasya.api.exception.ApiException;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class DisponibilidadServiceTest {
 HorarioDisponibleRepository horarios;BloqueoCanchaRepository bloqueos;CanchaRepository canchas;ReservaRepository reservas;DisponibilidadService service;
 Clock clock=Clock.fixed(Instant.parse("2026-10-09T15:00:00Z"),TimeConfig.ZONA);
 Cancha cancha;User dueno;
 @BeforeEach void setup(){horarios=mock(HorarioDisponibleRepository.class);bloqueos=mock(BloqueoCanchaRepository.class);canchas=mock(CanchaRepository.class);reservas=mock(ReservaRepository.class);service=new DisponibilidadService(horarios,bloqueos,canchas,reservas,clock);dueno=new User();dueno.setId(1L);cancha=new Cancha();cancha.setId(3L);cancha.setDueno(dueno);when(canchas.findById(3L)).thenReturn(Optional.of(cancha));when(canchas.findByIdForUpdate(3L)).thenReturn(Optional.of(cancha));}
 HorarioDisponible horario(String i,String f){var h=new HorarioDisponible();h.setCancha(cancha);h.setDiaSemana(6);h.setHoraInicio(LocalTime.parse(i));h.setHoraFin(LocalTime.parse(f));return h;}
 @Test void restaReservasYBloqueosSinExponerPersonas(){
  when(horarios.findByCanchaIdAndDiaSemana(3L,6)).thenReturn(List.of(horario("18:00","23:00")));
  var reserva=new Reserva();reserva.setEstado(EstadoReserva.CONFIRMADA);reserva.setHoraInicio(LocalTime.of(19,0));reserva.setHoraFin(LocalTime.of(20,0));
  when(reservas.findByCanchaIdAndFechaAndEstadoIn(anyLong(),any(),anyList())).thenReturn(List.of(reserva));
  var b=new BloqueoCancha();b.setHoraInicio(LocalTime.of(21,0));b.setHoraFin(LocalTime.of(22,0));when(bloqueos.findByCanchaIdAndFecha(3L,LocalDate.of(2026,10,10))).thenReturn(List.of(b));
  var response=service.consultar(3L,LocalDate.of(2026,10,10));
  assertThat(response.libres()).containsExactly(new FranjaResponse(LocalTime.of(18,0),LocalTime.of(19,0)),new FranjaResponse(LocalTime.of(20,0),LocalTime.of(21,0)),new FranjaResponse(LocalTime.of(22,0),LocalTime.of(23,0)));
  assertThat(FranjaResponse.class.getRecordComponents()).extracting(java.lang.reflect.RecordComponent::getName).containsExactly("horaInicio","horaFin");
 }
 @Test void sinAgendaNoSePuedeReservar(){assertThatThrownBy(()->service.validarReserva(3L,LocalDate.of(2026,10,10),LocalTime.of(18,0),LocalTime.of(19,0))).isInstanceOf(ApiException.class).hasMessageContaining("no publicó");}
 @Test void rechazaReservaFueraDelHorarioPublicado(){when(horarios.findByCanchaIdAndDiaSemana(3L,6)).thenReturn(List.of(horario("18:00","20:00")));assertThatThrownBy(()->service.validarReserva(3L,LocalDate.of(2026,10,10),LocalTime.of(19,30),LocalTime.of(20,30))).hasMessageContaining("no publicó");}
 @Test void cierreCompletoImpideReservar(){when(horarios.findByCanchaIdAndDiaSemana(3L,6)).thenReturn(List.of(horario("18:00","23:00")));when(bloqueos.findByCanchaIdAndFecha(anyLong(),any())).thenReturn(List.of(new BloqueoCancha()));assertThatThrownBy(()->service.validarReserva(3L,LocalDate.of(2026,10,10),LocalTime.of(18,0),LocalTime.of(19,0))).hasMessageContaining("bloqueó");}
 @Test void rechazaHorasPasadasEnArgentina(){assertThatThrownBy(()->service.validarReserva(3L,LocalDate.of(2026,10,9),LocalTime.of(11,0),LocalTime.of(12,0))).hasMessageContaining("pasado");}
 @Test void noPermiteGuardarAgendaDeOtroDueno(){var intruso=new User();intruso.setId(8L);assertThatThrownBy(()->service.reemplazar(3L,new AgendaRequest(List.of()),intruso)).hasMessageContaining("permiso");verify(horarios,never()).deleteByCanchaId(anyLong());}
 @Test void agendaSuperpuestaSeRechazaSinBorrarLaAnterior(){var a=new HorarioRequest();a.setDiaSemana(1);a.setHoraInicio(LocalTime.of(18,0));a.setHoraFin(LocalTime.of(21,0));var b=new HorarioRequest();b.setDiaSemana(1);b.setHoraInicio(LocalTime.of(20,0));b.setHoraFin(LocalTime.of(22,0));assertThatThrownBy(()->service.reemplazar(3L,new AgendaRequest(List.of(a,b)),dueno)).hasMessageContaining("superponerse");verify(horarios,never()).deleteByCanchaId(anyLong());}
 @Test void pendientesVencidasNoOcupan(){var r=new Reserva();r.setEstado(EstadoReserva.PENDIENTE);r.setExpiresAt(clock.instant().minusSeconds(1));assertThat(service.ocupa(r)).isFalse();r.setEstado(EstadoReserva.CONFIRMADA);assertThat(service.ocupa(r)).isTrue();}
 @Test void noAdmiteDuracionesFraccionadas(){assertThatThrownBy(()->service.validarReserva(3L,LocalDate.of(2026,10,10),LocalTime.of(18,0),LocalTime.of(18,31))).hasMessageContaining("30 minutos");}
}
