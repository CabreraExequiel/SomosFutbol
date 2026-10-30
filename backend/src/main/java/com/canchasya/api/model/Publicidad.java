package com.canchasya.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "publicidades", indexes = {
        @Index(name = "idx_publicidad_estado_ubicacion", columnList = "estado,ubicacion"),
        @Index(name = "idx_publicidad_ventana", columnList = "fecha_inicio,fecha_fin")
})
@Getter @Setter
public class Publicidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 400)
    private String descripcion;

    @Column(name = "imagen_url", length = 700)
    private String imagenUrl;

    @Column(name = "destino_url", nullable = false, length = 700)
    private String destinoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UbicacionPublicidad ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoPublicidad estado = EstadoPublicidad.BORRADOR;

    @Column(nullable = false)
    private Integer prioridad = 0;

    @Column(name = "fecha_inicio")
    private Instant fechaInicio;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "creado_por_id", nullable = false)
    private Long creadoPorId;

    @Column(name = "actualizado_por_id", nullable = false)
    private Long actualizadoPorId;
}
