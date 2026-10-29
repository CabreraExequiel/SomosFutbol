package com.canchasya.api.controller;

import com.canchasya.api.model.BitacoraEvento;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.BitacoraRepository;
import com.canchasya.api.service.BitacoraService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
            @RequestParam(required = false) String ambiente,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "0") int pagina) {
        if (pagina < 0 || pagina > 10000) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Página inválida");
        String n = filtro(nivel, "ERROR", "INFO");
        String o = filtro(origen, "BACKEND", "FRONTEND", "AUDITORIA");
        String a = filtro(ambiente, "local", "production");
        String e = filtro(estado, "PENDIENTE", "EN_INVESTIGACION", "RESUELTO");
        String termino = buscar == null ? "" : buscar.trim();
        if (termino.length() > 100 || termino.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Búsqueda inválida");
        }
        Specification<BitacoraEvento> spec = (root, query, cb) -> cb.conjunction();
        if (n != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("nivel"), n));
        if (o != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("origen"), o));
        if (a != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("ambiente"), a));
        if (e != null) {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.equal(root.get("nivel"), "ERROR"), cb.equal(root.get("estado"), e)));
        }
        if (!termino.isEmpty()) {
            // Escapar comodines SQL para buscar códigos/rutas literalmente y no permitir búsquedas masivas.
            String patron = "%" + termino.toLowerCase(java.util.Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("codigo")), patron, '!'),
                    cb.like(cb.lower(root.get("tipo")), patron, '!'),
                    cb.like(cb.lower(root.get("ruta")), patron, '!'),
                    cb.like(cb.lower(root.get("descripcion")), patron, '!')
            ));
        }
        var resultados = repository.findAll(spec, PageRequest.of(pagina, 25, Sort.Direction.DESC, "fecha", "id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(resultados.map(EventoRespuesta::from));
    }

    /** 09E4: indicadores por intervalo de días UTC, de solo lectura. La bitácora
     * registra eventos observados, no representa necesariamente todos los fallos de infraestructura.
     */
    @GetMapping("/api/admin/bitacora/estadisticas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<EstadisticasRespuesta> estadisticas(
            @RequestParam(defaultValue = "7") int dias,
            @RequestParam(required = false) String ambiente) {
        if (dias != 7 && dias != 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El período debe ser de 7 o 30 días");
        }
        String a = filtro(ambiente, "local", "production");
        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
        LocalDate primerDia = hoy.minusDays(dias - 1L);
        Instant inicio = primerDia.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant ahora = Instant.now();
        Specification<BitacoraEvento> intervalo = (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.<Instant>get("fecha"), inicio),
                cb.lessThanOrEqualTo(root.<Instant>get("fecha"), ahora));
        if (a != null) {
            intervalo = intervalo.and((root, query, cb) -> cb.equal(root.get("ambiente"), a));
        }
        Specification<BitacoraEvento> errores = intervalo.and(
                (root, query, cb) -> cb.equal(root.get("nivel"), "ERROR"));
        long total = repository.count(intervalo);
        long totalErrores = repository.count(errores);
        long pendientes = repository.count(errores.and(
                (root, query, cb) -> cb.equal(root.get("estado"), "PENDIENTE")));
        long investigacion = repository.count(errores.and(
                (root, query, cb) -> cb.equal(root.get("estado"), "EN_INVESTIGACION")));
        long resueltos = repository.count(errores.and(
                (root, query, cb) -> cb.equal(root.get("estado"), "RESUELTO")));
        long sinGestion = repository.count(errores.and(
                (root, query, cb) -> cb.isNull(root.get("atendidoPorId"))));
        long backend = repository.count(errores.and(
                (root, query, cb) -> cb.equal(root.get("origen"), "BACKEND")));
        long frontend = repository.count(errores.and(
                (root, query, cb) -> cb.equal(root.get("origen"), "FRONTEND")));
        long accionesAdmin = repository.count(intervalo.and((root, query, cb) -> cb.and(
                cb.equal(root.get("nivel"), "INFO"),
                root.get("tipo").in(TIPOS_ACCION_ADMIN))));

        // Siete barras diarias; para 30 días, seis barras de cinco días.
        int paso = dias == 7 ? 1 : 5;
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");
        List<TramoErrores> tendencia = new ArrayList<>();
        for (int offset = 0; offset < dias; offset += paso) {
            LocalDate desde = primerDia.plusDays(offset);
            LocalDate hasta = desde.plusDays(paso - 1L);
            Instant limiteInf = desde.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant limiteSup = hasta.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            // El límite superior de 'errores' evita incluir eventos futuros del día actual.
            long cantidad = repository.count(errores.and((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.<Instant>get("fecha"), limiteInf),
                    cb.lessThan(root.<Instant>get("fecha"), limiteSup))));
            String etiqueta = paso == 1 ? desde.format(formato) :
                    desde.format(formato) + "–" + hasta.format(formato);
            tendencia.add(new TramoErrores(etiqueta, cantidad));
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new EstadisticasRespuesta(dias, a == null ? "todos" : a, "UTC",
                        total, totalErrores, total - totalErrores, pendientes, investigacion,
                        resueltos, sinGestion, backend, frontend, accionesAdmin, tendencia));
    }

    public record TramoErrores(String etiqueta, long errores) { }
    public record EstadisticasRespuesta(int dias, String ambiente, String zonaHoraria,
            long totalEventos, long totalErrores, long eventosInformativos,
            long pendientes, long enInvestigacion, long resueltos, long sinGestion,
            long erroresBackend, long erroresFrontend, long accionesAdministrativas,
            List<TramoErrores> tendencia) { }

    /** 09E2: historial paginado de acciones administrativas ya registradas en bitácora.
     * Nunca incluye acciones habituales de jugadores o dueños.
     */
    private static final Set<String> TIPOS_ACCION_ADMIN = Set.of(
            "USUARIO_SUSPENDIDO", "USUARIO_REACTIVADO",
            "CANCHA_DESACTIVADA_ADMIN", "CANCHA_REACTIVADA_ADMIN",
            "COMPLEJO_SUSPENDIDO_ADMIN", "COMPLEJO_REACTIVADO_ADMIN",
            "INCIDENTE_ESTADO_CAMBIADO_ADMIN");

    @GetMapping("/api/admin/bitacora/acciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<EventoRespuesta>> listarAcciones(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long administradorId,
            @RequestParam(required = false) String buscar) {
        if (pagina < 0 || pagina > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida");
        }
        if (administradorId != null && administradorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrador inválido");
        }
        String t = tipo == null ? "" : tipo.trim();
        if (!t.isEmpty() && !TIPOS_ACCION_ADMIN.contains(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de acción inválido");
        }
        String termino = buscar == null ? "" : buscar.trim();
        if (termino.length() > 100 || termino.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Búsqueda inválida");
        }
        // Las suspensiones de cuenta antiguas se registraban como BACKEND; las
        // moderaciones como AUDITORIA. En ambos casos el tipo es restrictivo.
        Specification<BitacoraEvento> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("nivel"), "INFO"),
                root.get("tipo").in(TIPOS_ACCION_ADMIN),
                cb.or(cb.equal(root.get("origen"), "AUDITORIA"),
                      cb.and(cb.equal(root.get("origen"), "BACKEND"),
                             root.get("tipo").in("USUARIO_SUSPENDIDO", "USUARIO_REACTIVADO"))));
        if (!t.isEmpty()) spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), t));
        if (administradorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), administradorId));
        }
        if (!termino.isEmpty()) {
            String patron = "%" + termino.toLowerCase(java.util.Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("codigo")), patron, '!'),
                    cb.like(cb.lower(root.get("tipo")), patron, '!'),
                    cb.like(cb.lower(root.get("referenciaTipo")), patron, '!')));
        }
        var resultados = repository.findAll(spec,
                PageRequest.of(pagina, 15, Sort.Direction.DESC, "fecha", "id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(resultados.map(EventoRespuesta::from));
    }

    /** 09E3: cola de incidentes, independiente de eventos INFO y de auditoría. */
    @GetMapping("/api/admin/bitacora/incidentes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<EventoRespuesta>> listarIncidentes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String ambiente,
            @RequestParam(required = false) Long ultimoAdminId,
            @RequestParam(defaultValue = "false") boolean sinGestion,
            @RequestParam(required = false) String buscar) {
        if (pagina < 0 || pagina > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida");
        }
        if (ultimoAdminId != null && ultimoAdminId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrador inválido");
        }
        if (sinGestion && ultimoAdminId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtros de atención incompatibles");
        }
        String e = filtro(estado, "PENDIENTE", "EN_INVESTIGACION", "RESUELTO");
        String o = filtro(origen, "BACKEND", "FRONTEND");
        String a = filtro(ambiente, "local", "production");
        String termino = buscar == null ? "" : buscar.trim();
        if (termino.length() > 100 || termino.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Búsqueda inválida");
        }
        Specification<BitacoraEvento> spec = (root, query, cb) -> cb.equal(root.get("nivel"), "ERROR");
        if (e != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), e));
        if (o != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("origen"), o));
        if (a != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("ambiente"), a));
        if (sinGestion) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("atendidoPorId")));
        } else if (ultimoAdminId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("atendidoPorId"), ultimoAdminId));
        }
        if (!termino.isEmpty()) {
            String patron = "%" + termino.toLowerCase(java.util.Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("codigo")), patron, '!'),
                    cb.like(cb.lower(root.get("tipo")), patron, '!'),
                    cb.like(cb.lower(root.get("ruta")), patron, '!'),
                    cb.like(cb.lower(root.get("descripcion")), patron, '!')));
        }
        var resultados = repository.findAll(spec,
                PageRequest.of(pagina, 15, Sort.Direction.DESC, "fecha", "id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(resultados.map(EventoRespuesta::from));
    }

    /** Historial disponible: eventos de cambio de estado ya auditados, sin reconstruir estados anteriores. */
    @GetMapping("/api/admin/bitacora/{id}/historial")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<EventoRespuesta>> historialIncidente(
            @PathVariable Long id, @RequestParam(defaultValue = "0") int pagina) {
        if (id <= 0 || pagina < 0 || pagina > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parámetros inválidos");
        }
        BitacoraEvento incidente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente inexistente"));
        if (!"ERROR".equals(incidente.getNivel())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El registro no es un incidente");
        }
        Specification<BitacoraEvento> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("nivel"), "INFO"),
                cb.equal(root.get("origen"), "AUDITORIA"),
                cb.equal(root.get("tipo"), "INCIDENTE_ESTADO_CAMBIADO_ADMIN"),
                cb.equal(root.get("referenciaTipo"), "INCIDENTE"),
                cb.equal(root.get("referenciaId"), id));
        var historial = repository.findAll(spec,
                PageRequest.of(pagina, 10, Sort.Direction.DESC, "fecha", "id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(historial.map(EventoRespuesta::from));
    }

    /** Endpoint autenticado. No se reciben stack traces, mensajes o cuerpos HTTP. */
    @PostMapping("/api/diagnostico/frontend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CodigoRespuesta> errorFrontend(@Valid @RequestBody FrontendErrorRequest peticion,
                                                         @AuthenticationPrincipal User usuario) {
        // Limitación adicional por usuario implementada en FrontendReportLimiter.
        frontendReportLimiter.permitir(usuario.getId());
        String codigo = bitacora.errorFrontend(peticion.nombre(), peticion.ruta(), peticion.funcion(),
                peticion.clase(), peticion.archivo(), peticion.linea(), usuario.getId());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(new CodigoRespuesta(codigo));
    }

    /** El cambio de estado no edita el diagnóstico ni afecta la transacción de negocio. */
    @PatchMapping("/api/admin/bitacora/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ResponseEntity<EventoRespuesta> cambiarEstado(@PathVariable Long id,
                                                         @Valid @RequestBody CambiarEstadoRequest solicitud,
                                                         @AuthenticationPrincipal User administrador) {
        BitacoraEvento evento = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente inexistente"));
        if (!"ERROR".equals(evento.getNivel())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sólo se gestionan incidentes de error");
        }
        String estado = solicitud.estado();
        if (!"PENDIENTE".equals(estado) && !"EN_INVESTIGACION".equals(estado) && !"RESUELTO".equals(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido");
        }
        boolean cambioReal = !estado.equals(evento.getEstado());
        evento.setEstado(estado);
        evento.setAtendidoPorId(administrador.getId());
        evento.setAtendidoEn(Instant.now());
        BitacoraEvento actualizado = repository.saveAndFlush(evento);
        if (cambioReal) {
            // Registro posterior al commit para evitar auditar transacciones fallidas.
            bitacora.evento("INCIDENTE_ESTADO_CAMBIADO_ADMIN", "INCIDENTE", id, administrador.getId());
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(EventoRespuesta.from(actualizado));
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
                                  String causaProbable, String sqlState, Integer codigoSql,
                                  String estado, Long atendidoPorId, Instant atendidoEn) {
        static EventoRespuesta from(BitacoraEvento x) {
            return new EventoRespuesta(x.getId(), x.getFecha(), x.getNivel(), x.getOrigen(), x.getAmbiente(),
                    x.getTipo(), x.getCodigo(), x.getDescripcion(), x.getActorId(), x.getReferenciaTipo(),
                    x.getReferenciaId(), x.getRuta(),
                    x.getClaseOrigen(), x.getFuncionOrigen(), x.getArchivoOrigen(), x.getLineaOrigen(),
                    x.getCausaProbable(), x.getSqlState(), x.getCodigoSql(),
                    x.getEstado(), x.getAtendidoPorId(), x.getAtendidoEn());
        }
    }
    public record FrontendErrorRequest(@Size(max=70) String nombre, @Size(max=180) String ruta,
                                       @Size(max=120) String funcion, @Size(max=180) String clase,
                                       @Size(max=150) String archivo, Integer linea) { }
    public record CambiarEstadoRequest(@NotBlank String estado) { }
    public record CodigoRespuesta(String codigo) { }
}
