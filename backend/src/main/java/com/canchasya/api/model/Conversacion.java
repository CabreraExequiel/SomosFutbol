package com.canchasya.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversaciones", uniqueConstraints = @UniqueConstraint(columnNames = {"user_uno_id", "user_dos_id"}))
@Getter
@Setter
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_uno_id", nullable = false)
    private User userUno;

    @ManyToOne
    @JoinColumn(name = "user_dos_id", nullable = false)
    private User userDos;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
