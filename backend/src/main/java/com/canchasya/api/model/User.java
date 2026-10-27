package com.canchasya.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Identificador estable de Google: no confundir con el correo electrónico.
    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USUARIO;

    private String ciudad;

    @Column(length = 500)
    private String avatar;

    // Un dueño administra un complejo; es independiente de su nombre personal.
    @Column(name = "nombre_complejo", length = 150)
    private String nombreComplejo;

    @Column(name = "cbu_cvu", length = 22)
    private String cbuCvu;

    @Column(name = "alias_mercado_pago", length = 20)
    private String aliasMercadoPago;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "terms_version", length = 30)
    private String termsVersion;

    @Column(name = "privacy_accepted_at")
    private Instant privacyAcceptedAt;

    @Column(name = "privacy_version", length = 30)
    private String privacyVersion;

    @Column(name = "suspendido", nullable = false)
    private boolean suspendido = false;

    @Column(name = "motivo_suspension", length = 500)
    private String motivoSuspension;

    @Column(name = "suspendido_en")
    private Instant suspendidoEn;

    @Column(name = "suspendido_por_id")
    private Long suspendidoPorId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !suspendido;
    }
}
