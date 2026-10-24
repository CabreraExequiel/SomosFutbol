package com.canchasya.api.controller;

import com.canchasya.api.model.Role;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.CanchaRepository;
import com.canchasya.api.repository.PublicacionRepository;
import com.canchasya.api.repository.ReservaRepository;
import com.canchasya.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/resumen")
    public ResponseEntity<Resumen> resumen() {
        // Los roles y permisos se verifican en Spring Security y aquí nuevamente.
        List<UsuarioResumen> ultimos = users.findAll(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent().stream().map(UsuarioResumen::de).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Resumen(
                users.count(), canchas.count(), reservas.count(), publicaciones.count(), ultimos
        ));
    }

    public record UsuarioResumen(Long id, String nombre, String email, Role rol) {
        static UsuarioResumen de(User usuario) {
            return new UsuarioResumen(usuario.getId(), usuario.getName(), usuario.getEmail(), usuario.getRole());
        }
    }

    public record Resumen(long usuarios, long canchas, long reservas, long publicaciones,
                          List<UsuarioResumen> ultimosUsuarios) {}
}
