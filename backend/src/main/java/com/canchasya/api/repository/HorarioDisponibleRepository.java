package com.canchasya.api.repository;

import com.canchasya.api.model.HorarioDisponible;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioDisponibleRepository extends JpaRepository<HorarioDisponible, Long> {
    List<HorarioDisponible> findByCanchaIdAndDiaSemana(Long canchaId, Integer diaSemana);
    List<HorarioDisponible> findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(Long canchaId);
    void deleteByCanchaId(Long canchaId);
}

