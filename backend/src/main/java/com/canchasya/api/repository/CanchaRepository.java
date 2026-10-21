package com.canchasya.api.repository;

import com.canchasya.api.model.Cancha;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {

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
