package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/** Evita que un usuario autenticado sature el registro de errores frontend. */
@Component
public class FrontendReportLimiter {
    private final ConcurrentHashMap<Long, Ventana> ventanas = new ConcurrentHashMap<>();
    private record Ventana(long minuto, int cantidad) { }

    public void permitir(Long id) {
        long ahora = System.currentTimeMillis() / 60000;
        Ventana v = ventanas.compute(id, (key, anterior) -> {
            if (anterior == null || anterior.minuto() != ahora) return new Ventana(ahora, 1);
            return new Ventana(ahora, anterior.cantidad() + 1);
        });
        if (v.cantidad() > 5) throw new ApiException("Demasiados reportes, intentá más tarde", HttpStatus.TOO_MANY_REQUESTS);
        // Limpiar entradas vencidas ocasionalmente para evitar acumulación.
        if (ventanas.size() > 10000) ventanas.entrySet().removeIf(e -> e.getValue().minuto() < ahora - 2);
    }
}
