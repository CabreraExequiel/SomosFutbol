package com.canchasya.api.repository;

import com.canchasya.api.model.Amistad;
import com.canchasya.api.model.EstadoAmistad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AmistadRepository extends JpaRepository<Amistad, Long> {

    Optional<Amistad> findByPairKey(String pairKey);

    @Query("""
            select a from Amistad a
            where a.estado = :estado
              and (a.solicitante.id = :userId or a.destinatario.id = :userId)
            order by coalesce(a.respondedAt, a.createdAt) desc
            """)
    List<Amistad> findRelaciones(
            @Param("userId") Long userId,
            @Param("estado") EstadoAmistad estado
    );

    List<Amistad> findByDestinatarioIdAndEstadoOrderByCreatedAtDesc(Long userId, EstadoAmistad estado);

    List<Amistad> findBySolicitanteIdAndEstadoOrderByCreatedAtDesc(Long userId, EstadoAmistad estado);
}
