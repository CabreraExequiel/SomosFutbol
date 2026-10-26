package com.canchasya.api.repository;

import com.canchasya.api.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Query("select distinct c.publicacion.user.id from Comentario c where c.user.id = :userId")
    Set<Long> autoresComentados(@Param("userId") Long userId);

    // 05A3: contar comentarios por publicación sin cargar sus entidades.
    @Query("select c.publicacion.id, count(c.id) from Comentario c " +
            "where c.publicacion.id in :publicacionIds group by c.publicacion.id")
    java.util.List<Object[]> contarPorPublicaciones(
            @Param("publicacionIds") Set<Long> publicacionIds
    );
}
