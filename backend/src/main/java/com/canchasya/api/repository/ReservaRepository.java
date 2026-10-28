package com.canchasya.api.repository;

import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    long countByEstado(EstadoReserva estado);

    // 09D1: consulta administrativa paginada, de solo lectura. ManyToOne con EntityGraph
    // para evitar N+1 y acceder a nombres sin exponer entidades en la respuesta HTTP.
    @EntityGraph(attributePaths = {"user", "cancha", "cancha.dueno"})
    @Query(value = """
            select r from Reserva r
            join r.user u join r.cancha c join c.dueno d
            where (:estado is null or r.estado = :estado)
              and (:buscar = '' or lower(u.name) like lower(concat('%', :buscar, '%'))
                or lower(u.email) like lower(concat('%', :buscar, '%'))
                or lower(c.nombre) like lower(concat('%', :buscar, '%'))
                or lower(d.nombreComplejo) like lower(concat('%', :buscar, '%')))
            """,
            countQuery = """
            select count(r) from Reserva r
            join r.user u join r.cancha c join c.dueno d
            where (:estado is null or r.estado = :estado)
              and (:buscar = '' or lower(u.name) like lower(concat('%', :buscar, '%'))
                or lower(u.email) like lower(concat('%', :buscar, '%'))
                or lower(c.nombre) like lower(concat('%', :buscar, '%'))
                or lower(d.nombreComplejo) like lower(concat('%', :buscar, '%')))
            """)
    Page<Reserva> buscarAdministracion(@Param("buscar") String buscar,
                                       @Param("estado") EstadoReserva estado,
                                       Pageable pageable);



    // Bloqueo de fila dentro de la transaccion, compatible con MariaDB y MySQL.
    @Query(value = "SELECT r.* FROM reservas r WHERE r.id = :id FOR UPDATE", nativeQuery = true)
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    List<Reserva> findByUserIdOrderByFechaDesc(Long userId);

    List<Reserva> findByCanchaIdInOrderByFechaDesc(List<Long> canchaIds);

    List<Reserva> findByCanchaIdAndFechaAndEstadoIn(Long canchaId, LocalDate fecha, List<EstadoReserva> estados);
    @Query("select r.id from Reserva r where r.estado=com.canchasya.api.model.EstadoReserva.PENDIENTE and r.expiresAt<=:ahora")
    List<Long> pendientesVencidas(java.time.Instant ahora,org.springframework.data.domain.Pageable pageable);
}

