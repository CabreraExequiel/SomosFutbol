package com.canchasya.api.repository;

import com.canchasya.api.model.Pago;
import com.canchasya.api.model.EstadoPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Requerido por las metricas del dashboard administrativo (09A).
    long countByEstado(EstadoPago estado);

    /** 09D3: listado paginado de pagos, solo consulta administrativa. */
    @EntityGraph(attributePaths = {"reserva", "reserva.user", "reserva.cancha", "reserva.cancha.dueno"})
    @Query(value = """
            select p from Pago p
            join p.reserva r join r.user u join r.cancha c join c.dueno d
            where (:estado is null or p.estado = :estado)
              and (:buscar = '' or lower(u.name) like lower(concat('%', :buscar, '%'))
                or lower(u.email) like lower(concat('%', :buscar, '%'))
                or lower(c.nombre) like lower(concat('%', :buscar, '%'))
                or lower(coalesce(d.nombreComplejo, '')) like lower(concat('%', :buscar, '%'))
                or lower(coalesce(p.mpPaymentId, '')) like lower(concat('%', :buscar, '%'))
                or lower(coalesce(p.mpPreferenceId, '')) like lower(concat('%', :buscar, '%')))
            """,
            countQuery = """
            select count(p) from Pago p
            join p.reserva r join r.user u join r.cancha c join c.dueno d
            where (:estado is null or p.estado = :estado)
              and (:buscar = '' or lower(u.name) like lower(concat('%', :buscar, '%'))
                or lower(u.email) like lower(concat('%', :buscar, '%'))
                or lower(c.nombre) like lower(concat('%', :buscar, '%'))
                or lower(coalesce(d.nombreComplejo, '')) like lower(concat('%', :buscar, '%'))
                or lower(coalesce(p.mpPaymentId, '')) like lower(concat('%', :buscar, '%'))
                or lower(coalesce(p.mpPreferenceId, '')) like lower(concat('%', :buscar, '%')))
            """)
    Page<Pago> buscarAdministracion(@Param("buscar") String buscar,
                                   @Param("estado") EstadoPago estado,
                                   Pageable pageable);

    Optional<Pago> findByReservaId(Long reservaId);

    Optional<Pago> findByMpPaymentId(String mpPaymentId);

    List<Pago> findByReservaIdIn(Collection<Long> reservaIds);

    // Mantiene el bloqueo sin delegar la clausula FOR UPDATE a Hibernate.
    @Query(value = "SELECT p.* FROM pagos p WHERE p.id = :id FOR UPDATE", nativeQuery = true)
    Optional<Pago> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT p.* FROM pagos p WHERE p.reserva_id = :reservaId FOR UPDATE", nativeQuery = true)
    Optional<Pago> findByReservaIdForUpdate(@Param("reservaId") Long reservaId);
}
