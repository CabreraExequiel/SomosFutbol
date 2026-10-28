package com.canchasya.api.service;

import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Pago;
import com.canchasya.api.model.Reserva;
import com.canchasya.api.repository.PagoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 09D4: indicadores para revisión manual, basados ÚNICAMENTE en la base local.
 * No comprueba Mercado Pago ni cambia estados de pagos o reservas.
 */
@Service
@RequiredArgsConstructor
public class AdminConciliacionService {
    private static final int TAMANIO_PAGINA = 15;
    private final EntityManager entityManager;
    private final PagoRepository pagos;

    public enum TipoCaso {
        CONFIRMADA_SIN_PAGO_APROBADO,
        PAGO_APROBADO_RESERVA_NO_CONFIRMADA,
        PENDIENTE_VENCIDA_SIN_PAGO_APROBADO
    }

    private static final String CONFIRMADA_SIN_PAGO = """
            r.estado = :confirmada and r.pagoRequerido = true
            and not exists (select p.id from Pago p where p.reserva = r and p.estado = :aprobado)
            """;
    private static final String APROBADO_NO_CONFIRMADA = """
            r.estado <> :confirmada
            and exists (select p.id from Pago p where p.reserva = r and p.estado = :aprobado)
            """;
    private static final String PENDIENTE_VENCIDA = """
            r.estado = :pendiente and r.pagoRequerido = true
            and r.expiresAt is not null and r.expiresAt <= :ahora
            and not exists (select p.id from Pago p where p.reserva = r and p.estado = :aprobado)
            """;

    private String condicion(TipoCaso tipo) {
        return switch (tipo) {
            case CONFIRMADA_SIN_PAGO_APROBADO -> CONFIRMADA_SIN_PAGO;
            case PAGO_APROBADO_RESERVA_NO_CONFIRMADA -> APROBADO_NO_CONFIRMADA;
            case PENDIENTE_VENCIDA_SIN_PAGO_APROBADO -> PENDIENTE_VENCIDA;
        };
    }

    private <T> TypedQuery<T> parametros(TypedQuery<T> consulta, TipoCaso tipo, Instant ahora) {
        consulta.setParameter("aprobado", EstadoPago.APROBADO);
        if (tipo == TipoCaso.PENDIENTE_VENCIDA_SIN_PAGO_APROBADO) {
            consulta.setParameter("pendiente", EstadoReserva.PENDIENTE);
            consulta.setParameter("ahora", ahora);
        } else {
            consulta.setParameter("confirmada", EstadoReserva.CONFIRMADA);
        }
        return consulta;
    }

    private long contar(TipoCaso tipo, Instant ahora) {
        String jpql = "select count(r) from Reserva r where " + condicion(tipo);
        return parametros(entityManager.createQuery(jpql, Long.class), tipo, ahora)
                .getSingleResult();
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        Instant ahora = Instant.now();
        return new Resumen(
                contar(TipoCaso.CONFIRMADA_SIN_PAGO_APROBADO, ahora),
                contar(TipoCaso.PAGO_APROBADO_RESERVA_NO_CONFIRMADA, ahora),
                contar(TipoCaso.PENDIENTE_VENCIDA_SIN_PAGO_APROBADO, ahora),
                ahora);
    }

    @Transactional(readOnly = true)
    public PaginaCasos casos(TipoCaso tipo, int pagina) {
        // El tipo se resuelve con un enum; nunca se incorpora un valor libre a JPQL.
        int numero = Math.max(0, Math.min(pagina, 100000));
        Instant ahora = Instant.now();
        long total = contar(tipo, ahora);
        int totalPaginas = (int) Math.ceil((double) total / TAMANIO_PAGINA);
        // Si no hay resultados (o se solicita una pagina fuera de rango), no ejecutar
        // una segunda consulta JPQL innecesaria. Es una respuesta valida, no un error.
        if (total == 0 || (long) numero * TAMANIO_PAGINA >= total) {
            return new PaginaCasos(tipo, List.of(), numero, totalPaginas, total);
        }
        // Resolver las asociaciones dentro de la transaccion de lectura, sobre
        // un maximo de 15 reservas; evita las restricciones de fetch-join encadenados.
        String jpql = "select r from Reserva r where " + condicion(tipo) + " order by r.id desc";
        List<Reserva> reservas = parametros(
                entityManager.createQuery(jpql, Reserva.class), tipo, ahora)
                .setFirstResult(numero * TAMANIO_PAGINA)
                .setMaxResults(TAMANIO_PAGINA)
                .getResultList();
        List<Long> ids = reservas.stream().map(Reserva::getId).toList();
        Map<Long, Pago> porReserva = ids.isEmpty() ? Map.of() :
                pagos.findByReservaIdIn(ids).stream().collect(Collectors.toMap(
                        pago -> pago.getReserva().getId(), Function.identity(), (primero, segundo) -> primero));
        List<Caso> casos = reservas.stream()
                .map(reserva -> Caso.de(reserva, porReserva.get(reserva.getId()))).toList();
        return new PaginaCasos(tipo, casos, numero, totalPaginas, total);
    }

    public record Resumen(long confirmadasSinPagoAprobado,
                         long pagosAprobadosSinReservaConfirmada,
                         long pendientesVencidasSinPagoAprobado,
                         Instant consultadoEn) {}

    public record PaginaCasos(TipoCaso tipo, List<Caso> casos,
                             int pagina, int totalPaginas, long total) {}

    /** DTO acotado: no incluye checkoutUrl, tokens, datos bancarios o credenciales. */
    public record Caso(Long reservaId, Long pagoId, Long jugadorId, String jugador,
                       String correo, Long canchaId, String cancha, String complejo,
                       EstadoReserva estadoReserva, boolean pagoRequerido,
                       EstadoPago estadoPago, BigDecimal montoReserva, BigDecimal montoPago,
                       Instant vence) {
        static Caso de(Reserva r, Pago p) {
            String comercial = r.getCancha().getDueno().getNombreComplejo();
            return new Caso(r.getId(), p == null ? null : p.getId(), r.getUser().getId(),
                    r.getUser().getName(), r.getUser().getEmail(), r.getCancha().getId(),
                    r.getCancha().getNombre(),
                    comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                    r.getEstado(), r.isPagoRequerido(), p == null ? null : p.getEstado(),
                    r.getMonto(), p == null ? null : p.getMonto(), r.getExpiresAt());
        }
    }
}
