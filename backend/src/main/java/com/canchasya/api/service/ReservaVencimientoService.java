package com.canchasya.api.service;
import com.canchasya.api.model.*;
import com.canchasya.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
@Service @RequiredArgsConstructor
public class ReservaVencimientoService {
 private final ReservaRepository reservas;private final Clock clock;
 @Transactional public void vencer(Long id){reservas.findByIdForUpdate(id).ifPresent(r->{if(r.getEstado()==EstadoReserva.PENDIENTE && r.getExpiresAt()!=null && !r.getExpiresAt().isAfter(clock.instant())){r.setEstado(EstadoReserva.CANCELADA);reservas.save(r);}});}
}
