package com.canchasya.api.service;

import com.canchasya.api.model.Cancha;
import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Pago;
import com.canchasya.api.model.Reserva;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.PagoRepository;
import com.canchasya.api.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/** 09D2: lectura administrativa. No llama APIs externas ni modifica pagos o reservas. */
@Service
@RequiredArgsConstructor
public class AdminReservaConsultaService {
    private final ReservaRepository reservas;
    private final PagoRepository pagos;

    @Transactional(readOnly = true)
    public Optional<Detalle> detalle(Long id) {
        return reservas.findById(id).map(reserva -> {
            User jugador = reserva.getUser();
            Cancha cancha = reserva.getCancha();
            User propietario = cancha.getDueno();
            String comercial = propietario.getNombreComplejo();
            PagoDetalle pago = pagos.findByReservaId(id).map(PagoDetalle::de).orElse(null);
            return new Detalle(
                    reserva.getId(), jugador.getId(), jugador.getName(), jugador.getEmail(),
                    cancha.getId(), cancha.getNombre(),
                    comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                    reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin(),
                    reserva.getMonto(), reserva.getEstado(), reserva.isPagoRequerido(),
                    propietario.getId(), propietario.getName(), cancha.getCiudad().getNombre(),
                    cancha.getDireccion(), reserva.getCreatedAt(), reserva.getExpiresAt(), pago);
        });
    }

    public record Detalle(Long id, Long usuarioId, String usuario, String correo,
                          Long canchaId, String cancha, String complejo,
                          LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                          BigDecimal monto, EstadoReserva estado, boolean pagoRequerido,
                          Long propietarioId, String propietario, String ciudad, String direccion,
                          Instant creada, Instant vence, PagoDetalle pago) {}

    /** No se incluyen checkoutUrl, claves de cuentas, tokens ni credenciales. */
    public record PagoDetalle(Long id, BigDecimal monto, EstadoPago estado,
                              String metodo, LocalDateTime fechaPago,
                              String mpPaymentId, String mpPreferenceId) {
        static PagoDetalle de(Pago p) {
            return new PagoDetalle(p.getId(), p.getMonto(), p.getEstado(),
                    p.getMetodo(), p.getFechaPago(), p.getMpPaymentId(), p.getMpPreferenceId());
        }
    }
}
