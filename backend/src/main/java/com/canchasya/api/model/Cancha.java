package com.canchasya.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "canchas")
@Getter
@Setter
public class Cancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User dueno;

    @ManyToOne
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    @Column(nullable = false)
    private String nombre;

    private String direccion;

    private String tipo; // futbol 5, futbol 7, futbol 11

    private String superficie; // sintetico, natural

    private Integer cantJugadores;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioHora;

    private boolean techada;

    @Column(length = 500)
    private String imagen;

    @Column(length = 2000)
    private String descripcion;

    private boolean activa = true;
}
