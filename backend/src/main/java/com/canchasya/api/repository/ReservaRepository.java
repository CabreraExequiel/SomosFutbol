package com.canchasya.api.repository;

import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Bloqueo de fila dentro de la transaccion, compatible con MariaDB y MySQL.
    @Query(value = "SELECT r.* FROM reservas r WHERE r.id = :id FOR UPDATE", nativeQuery = true)
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    List<Reserva> findByUserIdOrderByFechaDesc(Long userId);

    List<Reserva> findByCanchaIdInOrderByFechaDesc(List<Long> canchaIds);

    List<Reserva> findByCanchaIdAndFechaAndEstadoIn(Long canchaId, LocalDate fecha, List<EstadoReserva> estados);
    @Query("select r.id from Reserva r where r.estado=com.canchasya.api.model.EstadoReserva.PENDIENTE and r.expiresAt<=:ahora")
    List<Long> pendientesVencidas(java.time.Instant ahora,org.springframework.data.domain.Pageable pageable);
}

