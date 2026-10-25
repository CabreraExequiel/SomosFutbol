package com.canchasya.api.repository;

import com.canchasya.api.model.Publicacion;
import com.canchasya.api.dto.FeedCandidato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.time.LocalDateTime;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    Page<Publicacion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Orden estable para navegar todas las publicaciones, sin el límite de 500.
    Slice<Publicacion> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    // Proyeccion ligera, sin gestionar entidades ni cargar medios/colecciones.
    // El orden secundario por ID hace estable la lectura de cada lote.
    @Query("select new com.canchasya.api.dto.FeedCandidato(" +
            "p.id, p.user.id, p.user.ciudad, p.createdAt) from Publicacion p " +
            "order by p.createdAt desc, p.id desc")
    Slice<FeedCandidato> listarCandidatosParaVos(Pageable pageable);

    // Keyset: los lotes posteriores no pagan OFFSET creciente.
    @Query("select new com.canchasya.api.dto.FeedCandidato(" +
            "p.id, p.user.id, p.user.ciudad, p.createdAt) from Publicacion p " +
            "where p.createdAt < :fecha or (p.createdAt = :fecha and p.id < :id) " +
            "order by p.createdAt desc, p.id desc")
    Slice<FeedCandidato> listarCandidatosParaVosAntesDe(
            @Param("fecha") LocalDateTime fecha,
            @Param("id") Long id,
            Pageable pageable);

    // 05B1: busca la etiqueta como palabra completa, evitando que #futbol
    // coincida por accidente con #futbol5. Funciona en MariaDB y MySQL.
    // El parametro patron es construido en el servicio tras validarlo.
    @Query(value = "select p.* from publicaciones p " +
            "where p.contenido REGEXP :patron " +
            "order by p.created_at desc, p.id desc", nativeQuery = true)
    Slice<Publicacion> buscarPorHashtag(
            @Param("patron") String patron,
            Pageable pageable);

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
