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

    Optional<Like> findByPublicacionIdAndUserId(Long publicacionId, Long userId);
}
