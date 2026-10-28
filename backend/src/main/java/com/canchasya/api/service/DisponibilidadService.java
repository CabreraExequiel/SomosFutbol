package com.canchasya.api.service;

import com.canchasya.api.config.TimeConfig;
import com.canchasya.api.dto.*;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class DisponibilidadService {
    private final HorarioDisponibleRepository horarios;
    private final BloqueoCanchaRepository bloqueos;
    private final CanchaRepository canchas;
    private final ReservaRepository reservas;
    private final Clock clock;
    private final ComplejoOperacionGuard complejoGuard;

    @Transactional(readOnly=true)
    public DisponibilidadResponse consultar(Long canchaId, LocalDate fecha) {
        Cancha cancha=canchas.findById(canchaId).orElseThrow(() -> error("Cancha no encontrada",404));
        if (!cancha.isActiva() || cancha.getDueno().isComplejoSuspendido())
            throw error("La cancha no está disponible",404);
        validarFecha(fecha);
        var publicados=horarios.findByCanchaIdAndDiaSemana(canchaId,fecha.getDayOfWeek().getValue()%7)
            .stream().map(h -> new FranjaResponse(h.getHoraInicio(),h.getHoraFin()))
            .sorted(Comparator.comparing(FranjaResponse::horaInicio)).toList();
        var ocupados=reservas.findByCanchaIdAndFechaAndEstadoIn(canchaId,fecha,
            List.of(EstadoReserva.PENDIENTE,EstadoReserva.CONFIRMADA)).stream()
            .filter(this::ocupa).map(r -> new FranjaResponse(r.getHoraInicio(),r.getHoraFin())).toList();
        var restar=new ArrayList<>(ocupados);
        for (var b:bloqueos.findByCanchaIdAndFecha(canchaId,fecha))
            restar.add(new FranjaResponse(b.getHoraInicio()==null?LocalTime.MIN:b.getHoraInicio(),
                b.getHoraFin()==null?LocalTime.MAX:b.getHoraFin()));
        if (fecha.equals(LocalDate.now(clock))) restar.add(new FranjaResponse(LocalTime.MIN,LocalTime.now(clock)));
        List<FranjaResponse> libres=new ArrayList<>(publicados);
        for (var corte:restar) libres=restar(libres,corte);
        libres=libres.stream().filter(f -> Duration.between(f.horaInicio(),f.horaFin()).toMinutes()>=30).toList();
        return new DisponibilidadResponse(canchaId,fecha,TimeConfig.ZONA.getId(),publicados,libres,ocupados);
    }

    public boolean ocupa(Reserva r) {
        return r.getEstado()==EstadoReserva.CONFIRMADA || r.getExpiresAt()==null || r.getExpiresAt().isAfter(clock.instant());
    }

    public void validarReserva(Long canchaId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        validarFecha(fecha);
        validarRango(inicio,fin);
        long minutos=Duration.between(inicio,fin).toMinutes();
        if (minutos<30 || minutos>240 || minutos%30!=0 || inicio.getMinute()%30!=0 || fin.getMinute()%30!=0)
            throw error("Elegí turnos de 30 minutos, entre 30 minutos y 4 horas",422);
        if (!LocalDateTime.of(fecha,inicio).isAfter(LocalDateTime.now(clock)))
            throw error("No se puede reservar un horario pasado",422);
        boolean permitido=horarios.findByCanchaIdAndDiaSemana(canchaId,fecha.getDayOfWeek().getValue()%7).stream()
            .anyMatch(h -> !inicio.isBefore(h.getHoraInicio()) && !fin.isAfter(h.getHoraFin()));
        if (!permitido) throw error("El dueño no publicó disponibilidad para ese día y horario",409);
        boolean cerrado=bloqueos.findByCanchaIdAndFecha(canchaId,fecha).stream()
            .anyMatch(b -> b.getHoraInicio()==null || (inicio.isBefore(b.getHoraFin()) && fin.isAfter(b.getHoraInicio())));
        if (cerrado) throw error("El dueño bloqueó ese día u horario",409);
    }

    @Transactional
    public List<HorarioResponse> reemplazar(Long id, AgendaRequest request, User user) {
        Cancha cancha=propietario(id,user);
        var lista=request.horarios();
        for(int i=0;i<lista.size();i++) {
            var h=lista.get(i); validarRango(h.getHoraInicio(),h.getHoraFin());
            if(h.getDiaSemana()<0 || h.getDiaSemana()>6) throw error("Día de la semana inválido",422);
            for(int j=0;j<i;j++) {
                var otro=lista.get(j);
                if(h.getDiaSemana().equals(otro.getDiaSemana()) && h.getHoraInicio().isBefore(otro.getHoraFin()) && h.getHoraFin().isAfter(otro.getHoraInicio()))
                    throw error("Los horarios de un mismo día no pueden superponerse",409);
            }
        }
        horarios.deleteByCanchaId(id); horarios.flush();
        for(var h:lista) {
            var nuevo=new HorarioDisponible();nuevo.setCancha(cancha);nuevo.setDiaSemana(h.getDiaSemana());
            nuevo.setHoraInicio(h.getHoraInicio());nuevo.setHoraFin(h.getHoraFin());horarios.save(nuevo);
        }
        return horarios.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(id).stream().map(HorarioResponse::fromEntity).toList();
    }

    @Transactional
    public BloqueoResponse bloquear(Long id, BloqueoRequest request, User user) {
        Cancha cancha=propietario(id,user);
        if(request.fecha().isBefore(LocalDate.now(clock)) || request.fecha().isAfter(LocalDate.now(clock).plusYears(1)))
            throw error("Elegí una fecha entre hoy y el próximo año",422);
        if(request.horaInicio()!=null || request.horaFin()!=null) validarRango(request.horaInicio(),request.horaFin());
        var b=new BloqueoCancha();b.setCancha(cancha);b.setFecha(request.fecha());b.setHoraInicio(request.horaInicio());
        b.setHoraFin(request.horaFin());b.setMotivo(request.motivo());
        return BloqueoResponse.from(bloqueos.save(b));
    }
    @Transactional(readOnly=true)
    public List<BloqueoResponse> bloqueos(Long id, User user) {
        Cancha cancha=canchas.findById(id).orElseThrow(()->error("Cancha no encontrada",404));validarDueno(cancha,user);
        return bloqueos.findByCanchaIdAndFechaGreaterThanEqualOrderByFechaAsc(id,LocalDate.now(clock)).stream().map(BloqueoResponse::from).toList();
    }
    @Transactional
    public void desbloquear(Long id, Long bloqueoId, User user) {
        propietario(id,user);
        var b=bloqueos.findById(bloqueoId).orElseThrow(()->error("Bloqueo no encontrado",404));
        if(!b.getCancha().getId().equals(id)) throw error("Bloqueo no encontrado",404);
        bloqueos.delete(b);
    }
    private Cancha propietario(Long id, User user) {
        return complejoGuard.bloquearCanchaDeDueno(id, user);
    }
    private void validarDueno(Cancha cancha, User user) {
        if(!cancha.getDueno().getId().equals(user.getId())) throw error("No tenés permiso sobre esta cancha",403);
    }
    private void validarFecha(LocalDate fecha) {
        if(fecha.isBefore(LocalDate.now(clock)) || fecha.isAfter(LocalDate.now(clock).plusDays(90)))
            throw error("Solo se puede consultar o reservar desde hoy hasta 90 días",422);
    }
    public static void validarRango(LocalTime inicio, LocalTime fin) {
        if(inicio==null || fin==null || !fin.isAfter(inicio) || inicio.getSecond()!=0 || fin.getSecond()!=0 || inicio.getNano()!=0 || fin.getNano()!=0)
            throw error("Ingresá un rango de horas válido dentro del mismo día",422);
    }
    static List<FranjaResponse> restar(List<FranjaResponse> base, FranjaResponse corte) {
        var resultado=new ArrayList<FranjaResponse>();
        for(var f:base) {
            if(!corte.horaInicio().isBefore(f.horaFin()) || !corte.horaFin().isAfter(f.horaInicio())) resultado.add(f);
            else {
                if(corte.horaInicio().isAfter(f.horaInicio())) resultado.add(new FranjaResponse(f.horaInicio(),corte.horaInicio()));
                if(corte.horaFin().isBefore(f.horaFin())) resultado.add(new FranjaResponse(corte.horaFin(),f.horaFin()));
            }
        }
        return resultado;
    }
    private static ApiException error(String mensaje,int codigo) {return new ApiException(mensaje,HttpStatus.valueOf(codigo));}
}
