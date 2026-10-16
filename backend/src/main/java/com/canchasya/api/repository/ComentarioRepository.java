package com.canchasya.api.repository;

import com.canchasya.api.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Query("select distinct c.publicacion.user.id from Comentario c where c.user.id = :userId")
    Set<Long> autoresComentados(@Param("userId") Long userId);
}
