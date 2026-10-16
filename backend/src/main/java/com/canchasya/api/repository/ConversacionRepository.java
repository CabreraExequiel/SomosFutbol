package com.canchasya.api.repository;

import com.canchasya.api.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    Optional<Conversacion> findByUserUnoIdAndUserDosId(Long userUnoId, Long userDosId);

    List<Conversacion> findByUserUnoIdOrUserDosIdOrderByUpdatedAtDesc(Long userUnoId, Long userDosId);
}
