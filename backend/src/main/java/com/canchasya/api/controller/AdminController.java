package com.canchasya.api.controller;

import com.canchasya.api.model.Role;
import com.canchasya.api.model.Cancha;
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

    public record PaginaCanchas(List<CanchaAdminResumen> canchas, int pagina, int totalPaginas, long total) {}

    public record CanchaAdminResumen(Long id, String nombre, String complejo, String ciudad,
                                    String propietario, String tipo, boolean activa) {
        static CanchaAdminResumen de(Cancha c) {
            String comercial = c.getDueno().getNombreComplejo();
            return new CanchaAdminResumen(c.getId(), c.getNombre(),
                    comercial == null || comercial.isBlank() ? "Sin nombre comercial" : comercial,
                    c.getCiudad().getNombre(), c.getDueno().getName(), c.getTipo(), c.isActiva());
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
