package com.canchasya.api.repository;

import com.canchasya.api.model.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    List<Mensaje> findByConversacionIdOrderByCreatedAtAsc(Long conversacionId);

    Optional<Mensaje> findTopByConversacionIdOrderByCreatedAtDesc(Long conversacionId);

    long countByConversacionIdAndUserIdNotAndLeidoFalse(Long conversacionId, Long userId);

    @Modifying
    @Query("""
            update Mensaje m set m.leido = true
            where m.conversacion.id = :conversacionId
              and m.user.id <> :userId
              and m.leido = false
            """)
    int marcarComoLeidos(
            @Param("conversacionId") Long conversacionId,
            @Param("userId") Long userId
    );
}
