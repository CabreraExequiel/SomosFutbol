package com.canchasya.api.service;

import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Pago;
import com.canchasya.api.model.Reserva;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 09D3: consulta en MySQL, sin modificar estados ni consultar a Mercado Pago. */
@Service
@RequiredArgsConstructor
public class AdminPagoConsultaService {
    private final PagoRepository pagos;

    @Transactional(readOnly = true)
    public PaginaPagos listar(String buscar, EstadoPago estado, int pagina) {
        String termino = buscar == null ? "" : buscar.strip();
        if (termino.length() > 100) termino = termino.substring(0, 100);
        int numero = Math.max(0, Math.min(pagina, 100000));
        Page<Pago> resultado = pagos.buscarAdministracion(termino, estado,
                PageRequest.of(numero, 15, Sort.by(Sort.Direction.DESC, "id")));
        return new PaginaPagos(resultado.getContent().stream().map(PagoResumen::de).toList(),
                resultado.getNumber(), resultado.getTotalPages(), resultado.getTotalElements());
    }

    public record PaginaPagos(java.util.List<PagoResumen> pagos, int pagina,
                              int totalPaginas, long total) {}

    /** Datos de soporte exclusivamente. Sin checkoutUrl, tokens ni cuentas de cobro. */
    public record PagoResumen(Long id, Long reservaId, Long usuarioId, String usuario,
                              String correo, Long canchaId, String cancha, String complejo,
                              BigDecimal monto, EstadoPago estado, EstadoReserva estadoReserva,
                              String metodo, LocalDateTime fechaPago,
                              String mpPaymentId, String mpPreferenceId) {
        static PagoResumen de(Pago p) {
            Reserva r = p.getReserva();
            User dueno = r.getCancha().getDueno();
            String comercial = dueno.getNombreComplejo();
            return new PagoResumen(p.getId(), r.getId(), r.getUser().getId(),
                    r.getUser().getName(), r.getUser().getEmail(),
                    r.getCancha().getId(), r.getCancha().getNombre(),
                    comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                    p.getMonto(), p.getEstado(), r.getEstado(), p.getMetodo(),
                    p.getFechaPago(), p.getMpPaymentId(), p.getMpPreferenceId());
        }
    }
}
