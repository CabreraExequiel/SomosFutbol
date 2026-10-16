package com.canchasya.api.repository;

import com.canchasya.api.model.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadRepository extends JpaRepository<Ciudad, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
}
