package com.canchasya.api.repository;

import com.canchasya.api.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReservaId(Long reservaId);

    Optional<Pago> findByMpPaymentId(String mpPaymentId);

    List<Pago> findByReservaIdIn(Collection<Long> reservaIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.reserva.id = :reservaId")
    Optional<Pago> findByReservaIdForUpdate(@Param("reservaId") Long reservaId);
}
