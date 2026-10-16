package com.canchasya.api.config;
import com.canchasya.api.repository.ReservaRepository;
import com.canchasya.api.service.ReservaVencimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.data.domain.PageRequest;
import java.time.Clock;
@Configuration @EnableScheduling @RequiredArgsConstructor
public class ReservaScheduler {
 private final ReservaRepository reservas;private final ReservaVencimientoService vencimiento;private final Clock clock;
 @Scheduled(fixedDelay=60000) public void vencer(){reservas.pendientesVencidas(clock.instant(),PageRequest.of(0,200)).forEach(vencimiento::vencer);}
}
