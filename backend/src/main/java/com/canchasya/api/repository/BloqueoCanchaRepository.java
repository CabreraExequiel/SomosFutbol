package com.canchasya.api.repository;
import com.canchasya.api.model.BloqueoCancha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface BloqueoCanchaRepository extends JpaRepository<BloqueoCancha,Long> {
    List<BloqueoCancha> findByCanchaIdAndFecha(Long canchaId, LocalDate fecha);
    List<BloqueoCancha> findByCanchaIdAndFechaGreaterThanEqualOrderByFechaAsc(Long canchaId, LocalDate fecha);
}
