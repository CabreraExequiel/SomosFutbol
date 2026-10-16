package com.canchasya.api.repository;

import com.canchasya.api.model.Notificacion;
import com.canchasya.api.model.TipoNotificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndLeidaFalse(Long userId);

    Optional<Notificacion> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.user.id = :userId and n.leida = false")
    int marcarTodasLeidas(@Param("userId") Long userId);

    @Modifying
    @Query("""
            update Notificacion n set n.leida = true
            where n.user.id = :userId
              and n.tipo = :tipo
              and n.referenciaId = :referenciaId
              and n.leida = false
            """)
    int marcarPorReferencia(
            @Param("userId") Long userId,
            @Param("tipo") TipoNotificacion tipo,
            @Param("referenciaId") Long referenciaId
    );
}
