package com.canchasya.api.repository;

import com.canchasya.api.model.Publicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
    Page<Publicacion> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
