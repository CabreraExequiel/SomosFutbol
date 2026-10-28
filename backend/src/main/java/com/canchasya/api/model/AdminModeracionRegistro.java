package com.canchasya.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Registro historico inmutable desde los endpoints administrativos. */
@Entity
@Table(name = "admin_moderacion_historial")
@Getter
@Setter
public class AdminModeracionRegistro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "referencia_tipo", nullable = false, length = 16)
    private String referenciaTipo;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(name = "estado_anterior", nullable = false, length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(length = 500)
    private String motivo;

    @Column(name = "administrador_id", nullable = false)
    private Long administradorId;

    @Column(name = "administrador_nombre", nullable = false, length = 150)
    private String administradorNombre;
}
