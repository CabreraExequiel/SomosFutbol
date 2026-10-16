package com.canchasya.api.service;

import com.canchasya.api.dto.*;
import com.canchasya.api.dto.auth.UserResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import com.canchasya.api.dto.ReservaResponse;
import com.canchasya.api.dto.DatosTransferenciaResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.CanchaRepository;
import com.canchasya.api.repository.PagoRepository;
import com.canchasya.api.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final CanchaRepository canchaRepository;
    private final PagoRepository pagoRepository;
    private final NotificacionService notificacionService;
    private final DisponibilidadService disponibilidadService;
    private final Clock clock;
    private final MercadoPagoCuentaService cuentasMp;

    @Value("${app.payments.enabled:false}")
    private boolean paymentsEnabled;

    public List<ReservaResponse> misReservas(User user) {
        return responder(reservaRepository.findByUserIdOrderByFechaDesc(user.getId()));
    }

    public List<ReservaResponse> reservasDeMisCanchas(User dueno) {
        List<Long> canchaIds = canchaRepository.findByDuenoId(dueno.getId()).stream()
                .map(Cancha::getId)
                .toList();
        return responder(reservaRepository.findByCanchaIdInOrderByFechaDesc(canchaIds));
    }

    @Transactional
    public ReservaResponse crear(ReservaRequest request, User user) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ApiException("El horario de fin debe ser posterior al de inicio", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getFecha().isBefore(LocalDate.now(clock))) {
            throw new ApiException("No se puede reservar una fecha pasada", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Cancha cancha = canchaRepository.findByIdForUpdate(request.getCanchaId())
                .orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));

        if (!cancha.isActiva()) {
            throw new ApiException("La cancha no está disponible", HttpStatus.CONFLICT);
        }
        if (paymentsEnabled && !cuentasMp.vinculada(cancha.getDueno().getId()))
            throw new ApiException("El dueño debe vincular su cuenta de Mercado Pago antes de recibir reservas",HttpStatus.CONFLICT);
        if (!paymentsEnabled && !tieneCuentaCobro(cancha.getDueno())) {
            throw new ApiException(
                    "El dueño debe configurar una cuenta para transferencias antes de recibir reservas",
                    HttpStatus.CONFLICT
            );
        }

        disponibilidadService.validarReserva(cancha.getId(),request.getFecha(),request.getHoraInicio(),request.getHoraFin());
        if (cancha.getDueno().getId().equals(user.getId())) throw new ApiException("No podés reservar tu propia cancha",HttpStatus.CONFLICT);

        List<Reserva> existentes = reservaRepository.findByCanchaIdAndFechaAndEstadoIn(
                cancha.getId(),
                request.getFecha(),
                List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA)
        );

        boolean superpuesta = existentes.stream().filter(disponibilidadService::ocupa).anyMatch(r ->
                request.getHoraInicio().isBefore(r.getHoraFin()) && request.getHoraFin().isAfter(r.getHoraInicio())
        );

        if (superpuesta) {
            throw new ApiException("Ese horario ya está reservado", HttpStatus.CONFLICT);
        }

        BigDecimal monto = calcularMonto(cancha.getPrecioHora(), request.getHoraInicio(), request.getHoraFin());

        Reserva reserva = new Reserva();
        reserva.setUser(user);
        reserva.setCancha(cancha);
        reserva.setFecha(request.getFecha());
        reserva.setHoraInicio(request.getHoraInicio());
        reserva.setHoraFin(request.getHoraFin());
        reserva.setMonto(monto);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setPagoRequerido(paymentsEnabled);
        reserva.setCreatedAt(clock.instant());
        var limite=clock.instant().plus(java.time.Duration.ofMinutes(paymentsEnabled?15:1440));
        var inicio=LocalDateTime.of(request.getFecha(),request.getHoraInicio()).atZone(clock.getZone()).toInstant();
        reserva.setExpiresAt(limite.isBefore(inicio)?limite:inicio);

        reservaRepository.save(reserva);
        notificacionService.crear(
                cancha.getDueno(),
                user,
                TipoNotificacion.RESERVA,
                "Nueva reserva",
                user.getName() + " reservó " + cancha.getNombre() + ".",
                "/mis-canchas",
                reserva.getId()
        );
        return ReservaResponse.fromEntity(reserva, null, paymentsEnabled);
    }

    public DisponibilidadResponse disponibilidad(Long canchaId, LocalDate fecha) {
        return disponibilidadService.consultar(canchaId,fecha);
    }

    public ReservaDetalleResponse detalle(Long id, User user) {
        Reserva reserva=obtener(id);validarAcceso(reserva,user);
        return new ReservaDetalleResponse(respuesta(reserva),UserResponse.fromEntity(reserva.getUser()),
            reserva.getCancha().getDireccion(),reserva.getCancha().getCiudad().getNombre());
    }

    public ReservaResponse ver(Long id, User user) {
        Reserva reserva = obtener(id);
        validarAcceso(reserva, user);
        return respuesta(reserva);
    }

    public DatosTransferenciaResponse datosTransferencia(Long id, User user) {
        Reserva reserva = obtener(id);
        validarAcceso(reserva, user);

        if (reserva.isPagoRequerido()) {
            throw new ApiException(
                    "Esta reserva debe pagarse mediante el checkout de Mercado Pago",
                    HttpStatus.CONFLICT
            );
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ApiException("La reserva está cancelada", HttpStatus.CONFLICT);
        }

        User dueno = reserva.getCancha().getDueno();
        if (!tieneCuentaCobro(dueno)) {
            throw new ApiException(
                    "El dueño todavía no configuró una cuenta para transferencias",
                    HttpStatus.CONFLICT
            );
        }

        return new DatosTransferenciaResponse(
                reserva.getId(),
                reserva.getCancha().getNombre(),
                dueno.getName(),
                reserva.getMonto(),
                dueno.getCbuCvu(),
                dueno.getAliasMercadoPago()
        );
    }

    @Transactional
    public ReservaResponse cancelar(Long id, User user) {
        Reserva reserva = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException("Reserva no encontrada", HttpStatus.NOT_FOUND));
        validarAcceso(reserva, user);

        pagoRepository.findByReservaIdForUpdate(id).ifPresent(pago -> {
            if (pago.getEstado() == EstadoPago.APROBADO) {
                throw new ApiException(
                        "Una reserva pagada no se puede cancelar directamente. Debe gestionarse el reembolso.",
                        HttpStatus.CONFLICT
                );
            }
        });

        if (reserva.getEstado()==EstadoReserva.CANCELADA) return respuesta(reserva);
        if (reserva.getEstado()==EstadoReserva.CONFIRMADA) throw new ApiException("Una reserva confirmada requiere coordinar la cancelación y el reintegro con el dueño",HttpStatus.CONFLICT);
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        User destinatario = reserva.getUser().getId().equals(user.getId())
                ? reserva.getCancha().getDueno()
                : reserva.getUser();
        notificacionService.crear(
                destinatario,
                user,
                TipoNotificacion.RESERVA,
                "Reserva cancelada",
                "Se canceló la reserva de " + reserva.getCancha().getNombre() + ".",
                destinatario.getId().equals(reserva.getUser().getId()) ? "/reservas" : "/mis-canchas",
                reserva.getId()
        );
        return respuesta(reserva);
    }

    @Transactional
    public ReservaResponse confirmar(Long id, User user) {
        Reserva referencia=obtener(id);validarAcceso(referencia,user);
        canchaRepository.findByIdForUpdate(referencia.getCancha().getId()).orElseThrow(() -> new ApiException("Cancha no encontrada",HttpStatus.NOT_FOUND));
        Reserva reserva = reservaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException("Reserva no encontrada", HttpStatus.NOT_FOUND));
        boolean esDueno = reserva.getCancha().getDueno().getId().equals(user.getId());
        if (!esDueno && user.getRole() != Role.ADMINISTRADOR) {
            throw new ApiException("Solo el propietario puede confirmar esta reserva", HttpStatus.FORBIDDEN);
        }
        if (reserva.getExpiresAt()!=null && !reserva.getExpiresAt().isAfter(clock.instant()))
            throw new ApiException("La reserva venció. El jugador debe crear una nueva",HttpStatus.CONFLICT);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ApiException("La reserva ya fue gestionada", HttpStatus.CONFLICT);
        }

        if (reserva.isPagoRequerido()) {
            EstadoPago estadoPago = pagoRepository.findByReservaIdForUpdate(id)
                    .map(Pago::getEstado)
                    .orElse(null);
            if (estadoPago != EstadoPago.APROBADO) {
                throw new ApiException(
                        "La reserva se confirmará automáticamente cuando se apruebe el pago",
                        HttpStatus.CONFLICT
                );
            }
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);
        notificacionService.crear(
                reserva.getUser(),
                user,
                TipoNotificacion.RESERVA,
                "Reserva confirmada",
                "Confirmaron tu reserva de " + reserva.getCancha().getNombre() + ".",
                "/reservas",
                reserva.getId()
        );
        return respuesta(reserva);
    }

    private BigDecimal calcularMonto(BigDecimal precioHora, LocalTime inicio, LocalTime fin) {
        return precioHora.multiply(BigDecimal.valueOf(Duration.between(inicio,fin).toMinutes()))
            .divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP);
    }

    private Reserva obtener(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ApiException("Reserva no encontrada", HttpStatus.NOT_FOUND));
    }

    private void validarAcceso(Reserva reserva, User user) {
        boolean esDueno = reserva.getCancha().getDueno().getId().equals(user.getId());
        boolean esPropia = reserva.getUser().getId().equals(user.getId());
        if (!esDueno && !esPropia && user.getRole() != Role.ADMINISTRADOR) {
            throw new ApiException("No tenés permiso sobre esta reserva", HttpStatus.FORBIDDEN);
        }
    }

    private boolean tieneCuentaCobro(User dueno) {
        return (dueno.getCbuCvu() != null && !dueno.getCbuCvu().isBlank())
                || (dueno.getAliasMercadoPago() != null && !dueno.getAliasMercadoPago().isBlank());
    }

    private List<ReservaResponse> responder(List<Reserva> reservas) {
        if (reservas.isEmpty()) return List.of();

        Map<Long, Pago> pagosPorReserva = pagoRepository.findByReservaIdIn(
                        reservas.stream().map(Reserva::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        pago -> pago.getReserva().getId(),
                        Function.identity()
                ));

        return reservas.stream()
                .map(reserva -> {
                    Pago pago = pagosPorReserva.get(reserva.getId());
                    return ReservaResponse.fromEntity(
                            reserva,
                            pago != null ? pago.getEstado() : null,
                            paymentsEnabled
                    );
                })
                .toList();
    }

    private ReservaResponse respuesta(Reserva reserva) {
        EstadoPago estadoPago = pagoRepository.findByReservaId(reserva.getId())
                .map(Pago::getEstado)
                .orElse(null);
        return ReservaResponse.fromEntity(reserva, estadoPago, paymentsEnabled);
    }
}
