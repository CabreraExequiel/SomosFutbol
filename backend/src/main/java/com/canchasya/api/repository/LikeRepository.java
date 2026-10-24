package com.canchasya.api.repository;

import com.canchasya.api.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikeRepository extends JpaRepository<Like, Long> {
    @Query("select distinct l.publicacion.user.id from Like l where l.user.id = :userId")
    Set<Long> autoresPreferidos(@Param("userId") Long userId);

    // Consulta conjunta para evitar una búsqueda por cada tarjeta del feed.
    @Query("select distinct l.publicacion.id from Like l " +
            "where l.user.id = :userId and l.publicacion.id in :publicacionIds")
    Set<Long> idsLikeadosPorUsuario(
            @Param("userId") Long userId,
            @Param("publicacionIds") Set<Long> publicacionIds
    );

    Optional<Like> findByPublicacionIdAndUserId(Long publicacionId, Long userId);
}
