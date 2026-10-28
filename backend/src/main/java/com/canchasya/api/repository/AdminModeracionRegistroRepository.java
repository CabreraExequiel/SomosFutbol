package com.canchasya.api.repository;

import com.canchasya.api.model.AdminModeracionRegistro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminModeracionRegistroRepository extends JpaRepository<AdminModeracionRegistro, Long> {
    Page<AdminModeracionRegistro> findByReferenciaTipoAndReferenciaId(
            String referenciaTipo, Long referenciaId, Pageable pageable);
}
