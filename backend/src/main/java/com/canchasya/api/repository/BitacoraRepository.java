package com.canchasya.api.repository;

import com.canchasya.api.model.BitacoraEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BitacoraRepository extends JpaRepository<BitacoraEvento, Long>, JpaSpecificationExecutor<BitacoraEvento> { }
