package com.canchasya.api.repository;

import com.canchasya.api.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    Page<Publicacion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Orden estable para navegar todas las publicaciones, sin el límite de 500.
    Slice<Publicacion> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    // 05A4: la base filtra los autores del feed Amigos y pagina sin el límite de 500.
    // Se trae el autor con la publicación para evitar consultas N+1 de perfiles.
    @EntityGraph(attributePaths = "user")
    @Query("select p from Publicacion p where p.user.id in :autorIds " +
            "order by p.createdAt desc, p.id desc")
    Slice<Publicacion> findFeedAmigos(
            @Param("autorIds") Set<Long> autorIds,
            Pageable pageable
    );
}
