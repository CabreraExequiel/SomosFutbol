package com.canchasya.api.controller;

import com.canchasya.api.model.Role;
import com.canchasya.api.model.Cancha;
import com.canchasya.api.model.Reserva;
import com.canchasya.api.model.EstadoReserva;
import com.canchasya.api.model.EstadoPago;
import com.canchasya.api.repository.PagoRepository;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.CanchaRepository;
import com.canchasya.api.repository.PublicacionRepository;
import com.canchasya.api.repository.ReservaRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import com.canchasya.api.service.AdminSuspensionService;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Primera entrega del panel administrativo: lectura solamente y datos limitados. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository users;
    private final CanchaRepository canchas;
    private final ReservaRepository reservas;
    private final PublicacionRepository publicaciones;
    private final PagoRepository pagos;
    private final AdminSuspensionService suspensiones;
    private final com.canchasya.api.service.AdminReservaConsultaService consultaReservas;
    private final com.canchasya.api.service.AdminPagoConsultaService consultaPagos;
    private final com.canchasya.api.service.AdminConciliacionService conciliacion;
    private final com.canchasya.api.service.AdminCanchaModeracionService moderacionCanchas;
    private final com.canchasya.api.service.AdminComplejoModeracionService moderacionComplejos;
    private final com.canchasya.api.service.AdminModeracionHistorialService historialModeracion;

    @GetMapping("/resumen")
    public ResponseEntity<Resumen> resumen() {
        // Los roles y permisos se verifican en Spring Security y aquí nuevamente.
        List<UsuarioResumen> ultimos = users.findAll(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent().stream().map(UsuarioResumen::de).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Resumen(
                users.count(), canchas.count(), reservas.count(), publicaciones.count(), ultimos,
                canchas.countByActivaTrue(),
                reservas.countByEstado(EstadoReserva.PENDIENTE),
                reservas.countByEstado(EstadoReserva.CONFIRMADA),
                reservas.countByEstado(EstadoReserva.CANCELADA),
                pagos.count(),
                pagos.countByEstado(EstadoPago.PENDIENTE),
                pagos.countByEstado(EstadoPago.APROBADO),
                pagos.countByEstado(EstadoPago.RECHAZADO),
                pagos.countByEstado(EstadoPago.REEMBOLSADO)
        ));
    }

    /** Listado seguro: nunca serializar entidades User ni datos de autenticación. */
    @GetMapping("/usuarios")
    public ResponseEntity<PaginaUsuarios> usuarios(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(required = false) Role rol,
            @RequestParam(defaultValue = "0") int pagina) {
        String termino = buscar == null ? "" : buscar.strip();
        if (termino.length() > 100) termino = termino.substring(0, 100);
        int numero = Math.max(0, Math.min(pagina, 100000));
        Page<User> resultado = users.buscarAdministracion(
                termino, rol, PageRequest.of(numero, 15, Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PaginaUsuarios(
                resultado.getContent().stream().map(UsuarioResumen::de).toList(),
                resultado.getNumber(), resultado.getTotalPages(), resultado.getTotalElements()));
    }

    /** Detalle administrativo limitado: no exponer contraseña, tokens ni datos bancarios. */
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDetalle> detalleUsuario(@PathVariable Long id) {
        return users.findById(id)
                .map(u -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                        new UsuarioDetalle(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                                u.getCiudad(), u.getNombreComplejo(), u.getGoogleSub() != null,
                                u.isSuspendido(), u.getMotivoSuspension())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record UsuarioDetalle(Long id, String nombre, String email, Role rol,
                                 String ciudad, String nombreComplejo, boolean googleVinculado,
                                 boolean suspendido, String motivoSuspension) {}

    public record EstadoCuentaRequest(boolean suspendido, String motivo) {}

    @PostMapping("/usuarios/{id}/estado")
    public ResponseEntity<UsuarioDetalle> cambiarEstadoCuenta(@PathVariable Long id,
            @RequestBody EstadoCuentaRequest solicitud, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        User actualizado = suspensiones.actualizar(id, actor.getId(), solicitud.suspendido(), solicitud.motivo());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                new UsuarioDetalle(actualizado.getId(), actualizado.getName(), actualizado.getEmail(),
                        actualizado.getRole(), actualizado.getCiudad(), actualizado.getNombreComplejo(),
                        actualizado.getGoogleSub() != null, actualizado.isSuspendido(), actualizado.getMotivoSuspension()));
    }

    /** 09D1: listado administrativo de reservas, sin operaciones de pago/cancelación. */
    @GetMapping("/reservas")
    public ResponseEntity<PaginaReservas> reservasAdmin(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(defaultValue = "0") int pagina) {
        String termino = buscar == null ? "" : buscar.strip();
        if (termino.length() > 100) termino = termino.substring(0, 100);
        int numero = Math.max(0, Math.min(pagina, 100000));
        Page<Reserva> resultado = reservas.buscarAdministracion(termino, estado,
                PageRequest.of(numero, 15, Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PaginaReservas(
                resultado.getContent().stream().map(AdminReservaResumen::de).toList(),
                resultado.getNumber(), resultado.getTotalPages(), resultado.getTotalElements()));
    }

    public record PaginaReservas(List<AdminReservaResumen> reservas,
                                 int pagina, int totalPaginas, long total) {}

    public record AdminReservaResumen(Long id, Long usuarioId, String usuario,
                                     String correo, Long canchaId, String cancha,
                                     String complejo, java.time.LocalDate fecha,
                                     java.time.LocalTime horaInicio, java.time.LocalTime horaFin,
                                     java.math.BigDecimal monto, EstadoReserva estado,
                                     boolean pagoRequerido) {
        static AdminReservaResumen de(Reserva r) {
            String comercial = r.getCancha().getDueno().getNombreComplejo();
            return new AdminReservaResumen(r.getId(), r.getUser().getId(), r.getUser().getName(),
                    r.getUser().getEmail(), r.getCancha().getId(), r.getCancha().getNombre(),
                    comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                    r.getFecha(), r.getHoraInicio(), r.getHoraFin(), r.getMonto(),
                    r.getEstado(), r.isPagoRequerido());
        }
    }

    /** 09D2: ficha de reserva y pago asociado; no altera estados ni contacta a Mercado Pago. */
    @GetMapping("/reservas/{id}")
    public ResponseEntity<com.canchasya.api.service.AdminReservaConsultaService.Detalle>
            detalleReserva(@PathVariable Long id) {
        return consultaReservas.detalle(id)
                .map(detalle -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(detalle))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 09D3: pagos registrados en MySQL; no verifica transacciones externas. */
    @GetMapping("/pagos")
    public ResponseEntity<com.canchasya.api.service.AdminPagoConsultaService.PaginaPagos> pagosAdmin(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(defaultValue = "0") int pagina) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(consultaPagos.listar(buscar, estado, pagina));
    }

    /** 09D4: revisión de diferencias entre estados persistidos en MySQL. */
    @GetMapping("/conciliacion/resumen")
    public ResponseEntity<com.canchasya.api.service.AdminConciliacionService.Resumen> resumenConciliacion() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(conciliacion.resumen());
    }

    /** Los casos no son veredictos de pago; requieren verificación manual. */
    @GetMapping("/conciliacion/casos")
    public ResponseEntity<com.canchasya.api.service.AdminConciliacionService.PaginaCasos> casosConciliacion(
            @RequestParam com.canchasya.api.service.AdminConciliacionService.TipoCaso tipo,
            @RequestParam(defaultValue = "0") int pagina) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(conciliacion.casos(tipo, pagina));
    }

    /** Consulta paginada de canchas sin exponer entidades o datos privados de dueños. */
    @GetMapping("/canchas")
    public ResponseEntity<PaginaCanchas> canchasAdmin(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(required = false) Boolean activa,
            @RequestParam(defaultValue = "0") int pagina) {
        String termino = buscar == null ? "" : buscar.strip();
        if (termino.length() > 100) termino = termino.substring(0, 100);
        int numero = Math.max(0, Math.min(pagina, 100000));
        Page<Cancha> resultado = canchas.buscarAdministracion(termino, activa,
                PageRequest.of(numero, 15, Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PaginaCanchas(
                resultado.getContent().stream().map(CanchaAdminResumen::de).toList(),
                resultado.getNumber(), resultado.getTotalPages(), resultado.getTotalElements()));
    }

    /** Detalle administrativo sin serializar entidades ni modificar el estado de la cancha. */
    @GetMapping("/canchas/{id}")
    public ResponseEntity<CanchaAdminDetalle> detalleCancha(@PathVariable Long id) {
        return canchas.findById(id)
                .map(c -> ResponseEntity.ok().cacheControl(CacheControl.noStore())
                        .body(CanchaAdminDetalle.de(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record EstadoCanchaRequest(boolean activa, String motivo) {}

    @PostMapping("/canchas/{id}/estado")
    public ResponseEntity<CanchaAdminDetalle> cambiarEstadoCancha(@PathVariable Long id,
            @RequestBody EstadoCanchaRequest solicitud, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        Cancha cancha = moderacionCanchas.actualizar(id, actor.getId(), solicitud.activa(), solicitud.motivo());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(CanchaAdminDetalle.de(cancha));
    }

    /** Moderación integral: se almacena en el dueño, sin cambiar estados individuales de canchas. */
    @GetMapping("/complejos/{duenoId}")
    public ResponseEntity<com.canchasya.api.service.AdminComplejoModeracionService.ComplejoAdminDetalle>
            detalleComplejo(@PathVariable Long duenoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(moderacionComplejos.detalle(duenoId));
    }

    public record EstadoComplejoRequest(boolean suspendido, String motivo) {}

    @PostMapping("/complejos/{duenoId}/estado")
    public ResponseEntity<com.canchasya.api.service.AdminComplejoModeracionService.ComplejoAdminDetalle>
            cambiarEstadoComplejo(@PathVariable Long duenoId,
                    @RequestBody EstadoComplejoRequest solicitud, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(moderacionComplejos.actualizar(duenoId, actor.getId(),
                        solicitud.suspendido(), solicitud.motivo()));
    }

    /** Historial duradero (09C5). Solo administradores; sin exponer entidades JPA. */
    @GetMapping("/canchas/{id}/historial")
    public ResponseEntity<Page<com.canchasya.api.service.AdminModeracionHistorialService.RegistroRespuesta>>
            historialCancha(@PathVariable Long id, @RequestParam(defaultValue = "0") int pagina) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(historialModeracion.listar("CANCHA", id, pagina));
    }

    @GetMapping("/complejos/{duenoId}/historial")
    public ResponseEntity<Page<com.canchasya.api.service.AdminModeracionHistorialService.RegistroRespuesta>>
            historialComplejo(@PathVariable Long duenoId, @RequestParam(defaultValue = "0") int pagina) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(historialModeracion.listar("COMPLEJO", duenoId, pagina));
    }

    public record CanchaAdminDetalle(Long id, String nombre, String complejo, String ciudad,
                                    Long propietarioId, String propietario, String tipo,
                                    String superficie, Integer cantJugadores,
                                    java.math.BigDecimal precioHora, boolean techada,
                                    String direccion, String descripcion, boolean activa,
                                    String motivoDesactivacionAdmin, boolean complejoSuspendido) {
        static CanchaAdminDetalle de(Cancha c) {
            String comercial = c.getDueno().getNombreComplejo();
            return new CanchaAdminDetalle(c.getId(), c.getNombre(),
                    comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                    c.getCiudad().getNombre(), c.getDueno().getId(), c.getDueno().getName(),
                    c.getTipo(), c.getSuperficie(), c.getCantJugadores(), c.getPrecioHora(),
                    c.isTechada(), c.getDireccion(), c.getDescripcion(), c.isActiva(),
                    c.getMotivoDesactivacionAdmin(), c.getDueno().isComplejoSuspendido());
        }
    }

    public record PaginaCanchas(List<CanchaAdminResumen> canchas, int pagina, int totalPaginas, long total) {}

    public record CanchaAdminResumen(Long id, String nombre, String complejo, String ciudad,
                                    String propietario, String tipo, boolean activa, boolean complejoSuspendido) {
        static CanchaAdminResumen de(Cancha c) {
            String comercial = c.getDueno().getNombreComplejo();
            return new CanchaAdminResumen(c.getId(), c.getNombre(),
                    comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                    c.getCiudad().getNombre(), c.getDueno().getName(), c.getTipo(), c.isActiva(),
                    c.getDueno().isComplejoSuspendido());
        }
    }

    public record PaginaUsuarios(List<UsuarioResumen> usuarios, int pagina, int totalPaginas, long total) {}

    public record UsuarioResumen(Long id, String nombre, String email, Role rol) {
        static UsuarioResumen de(User usuario) {
            return new UsuarioResumen(usuario.getId(), usuario.getName(), usuario.getEmail(), usuario.getRole());
        }
    }

    public record Resumen(long usuarios, long canchas, long reservas, long publicaciones,
                          List<UsuarioResumen> ultimosUsuarios, long canchasActivas,
                          long reservasPendientes, long reservasConfirmadas, long reservasCanceladas,
                          long pagosTotales, long pagosPendientes, long pagosAprobados,
                          long pagosRechazados, long pagosReembolsados) {}
}
