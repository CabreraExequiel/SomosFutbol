package com.canchasya.api.controller;

import com.canchasya.api.model.BitacoraEvento;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.BitacoraRepository;
import com.canchasya.api.service.BitacoraService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class BitacoraController {
    private final BitacoraRepository repository;
    private final BitacoraService bitacora;

    @GetMapping("/api/admin/bitacora")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<EventoRespuesta>> listar(
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String origen,
            @RequestParam(defaultValue = "0") int pagina) {
        if (pagina < 0 || pagina > 10000) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Página inválida");
        String n = filtro(nivel, "ERROR", "INFO");
        String o = filtro(origen, "BACKEND", "FRONTEND", "AUDITORIA");
        Specification<BitacoraEvento> spec = (root, query, cb) -> cb.conjunction();
        if (n != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("nivel"), n));
        if (o != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("origen"), o));
        var resultados = repository.findAll(spec, PageRequest.of(pagina, 25, Sort.Direction.DESC, "fecha", "id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(resultados.map(EventoRespuesta::from));
    }

    /** Endpoint autenticado. No se reciben stack traces, mensajes o cuerpos HTTP. */
    @PostMapping("/api/diagnostico/frontend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CodigoRespuesta> errorFrontend(@Valid @RequestBody FrontendErrorRequest peticion,
                                                         @AuthenticationPrincipal User usuario) {
        // Limitación adicional por usuario implementada en FrontendReportLimiter.
        frontendReportLimiter.permitir(usuario.getId());
        String codigo = bitacora.errorFrontend(peticion.nombre(), peticion.ruta(), peticion.funcion(), usuario.getId());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(new CodigoRespuesta(codigo));
    }

    private final com.canchasya.api.service.FrontendReportLimiter frontendReportLimiter;

    private static String filtro(String valor, String... permitidos) {
        if (valor == null || valor.isBlank()) return null;
        for (var permitido : permitidos) if (permitido.equals(valor)) return valor;
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Filtro inválido");
    }

    public record EventoRespuesta(Long id, Instant fecha, String nivel, String origen, String ambiente,
                                  String tipo, String codigo, String descripcion, Long actorId,
                                  String referenciaTipo, Long referenciaId, String ruta,
                                  String claseOrigen, String funcionOrigen, String archivoOrigen, Integer lineaOrigen,
                                  String causaProbable, String sqlState, Integer codigoSql) {
        static EventoRespuesta from(BitacoraEvento x) {
            return new EventoRespuesta(x.getId(), x.getFecha(), x.getNivel(), x.getOrigen(), x.getAmbiente(),
                    x.getTipo(), x.getCodigo(), x.getDescripcion(), x.getActorId(), x.getReferenciaTipo(),
                    x.getReferenciaId(), x.getRuta(),
                    x.getClaseOrigen(), x.getFuncionOrigen(), x.getArchivoOrigen(), x.getLineaOrigen(),
                    x.getCausaProbable(), x.getSqlState(), x.getCodigoSql());
        }
    }
    public record FrontendErrorRequest(@Size(max=70) String nombre, @Size(max=180) String ruta,
                                       @Size(max=120) String funcion) { }
    public record CodigoRespuesta(String codigo) { }
}
