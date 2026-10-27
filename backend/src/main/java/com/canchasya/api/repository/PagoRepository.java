package com.canchasya.api.repository;

import com.canchasya.api.model.Pago;
import com.canchasya.api.model.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    long countByEstado(EstadoPago estado);


    Optional<Pago> findByReservaId(Long reservaId);

    Optional<Pago> findByMpPaymentId(String mpPaymentId);

    List<Pago> findByReservaIdIn(Collection<Long> reservaIds);

    // Mantiene el bloqueo sin delegar la clausula FOR UPDATE a Hibernate.
    @Query(value = "SELECT p.* FROM pagos p WHERE p.id = :id FOR UPDATE", nativeQuery = true)
    Optional<Pago> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT p.* FROM pagos p WHERE p.reserva_id = :reservaId FOR UPDATE", nativeQuery = true)
    Optional<Pago> findByReservaIdForUpdate(@Param("reservaId") Long reservaId);
}
