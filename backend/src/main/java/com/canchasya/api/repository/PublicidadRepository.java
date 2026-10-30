package com.canchasya.api.repository;

import com.canchasya.api.model.Publicidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PublicidadRepository extends JpaRepository<Publicidad, Long>, JpaSpecificationExecutor<Publicidad> {
}
