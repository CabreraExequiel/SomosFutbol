package com.canchasya.api.repository;

import com.canchasya.api.model.Cancha;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"dueno", "ciudad"})
    @Query(value = """
            select c from Cancha c join c.dueno d join c.ciudad ci
            where (:estado is null or c.activa = :estado)
              and (:termino = '' or lower(c.nombre) like lower(concat('%', :termino, '%'))
                or lower(coalesce(d.nombreComplejo, '')) like lower(concat('%', :termino, '%'))
                or lower(d.name) like lower(concat('%', :termino, '%'))
                or lower(ci.nombre) like lower(concat('%', :termino, '%')))
            """,
           countQuery = """
            select count(c) from Cancha c join c.dueno d join c.ciudad ci
            where (:estado is null or c.activa = :estado)
              and (:termino = '' or lower(c.nombre) like lower(concat('%', :termino, '%'))
                or lower(coalesce(d.nombreComplejo, '')) like lower(concat('%', :termino, '%'))
                or lower(d.name) like lower(concat('%', :termino, '%'))
                or lower(ci.nombre) like lower(concat('%', :termino, '%')))
            """)
    org.springframework.data.domain.Page<Cancha> buscarAdministracion(
            @Param("termino") String termino, @Param("estado") Boolean estado,
            org.springframework.data.domain.Pageable pageable);

    // No cargar Cancha en el contexto JPA antes de adquirir su bloqueo FOR UPDATE.
    @Query(value = "SELECT user_id FROM canchas WHERE id = :id", nativeQuery = true)
    java.util.Optional<Long> findDuenoIdByCanchaId(@Param("id") Long id);

    long countByActivaTrue();


    // SQL nativa: MariaDB 10.4 admite FOR UPDATE pero no FOR UPDATE OF <alias>.
    // El servicio llamante debe mantener una transaccion @Transactional activa.
    @Query(value = "SELECT c.* FROM canchas c WHERE c.id = :id FOR UPDATE", nativeQuery = true)
    java.util.Optional<Cancha> findByIdForUpdate(@Param("id") Long id);

    List<Cancha> findByActivaTrueAndCiudadId(Long ciudadId);

    List<Cancha> findByActivaTrueAndTipo(String tipo);

    List<Cancha> findByActivaTrueAndCiudadIdAndTipo(Long ciudadId, String tipo);

    List<Cancha> findByActivaTrue();

    List<Cancha> findByDuenoId(Long duenoId);
}
