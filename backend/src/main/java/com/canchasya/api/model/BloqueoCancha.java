package com.canchasya.api.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity @Table(name="bloqueos_cancha") @Getter @Setter
public class BloqueoCancha {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne @JoinColumn(name="cancha_id",nullable=false) private Cancha cancha;
    @Column(nullable=false) private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    @Column(length=200) private String motivo;
}
