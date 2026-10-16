package com.canchasya.api.repository;

import com.canchasya.api.model.Cancha;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cancha c where c.id = :id")
    java.util.Optional<Cancha> findByIdForUpdate(@Param("id") Long id);

    List<Cancha> findByActivaTrueAndCiudadId(Long ciudadId);

    List<Cancha> findByActivaTrueAndTipo(String tipo);

    List<Cancha> findByActivaTrueAndCiudadIdAndTipo(Long ciudadId, String tipo);

    List<Cancha> findByActivaTrue();

    List<Cancha> findByDuenoId(Long duenoId);
}
