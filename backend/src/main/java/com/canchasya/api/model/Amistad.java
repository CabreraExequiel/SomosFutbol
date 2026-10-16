package com.canchasya.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "amistades",
        uniqueConstraints = @UniqueConstraint(name = "uk_amistad_pair", columnNames = "pair_key"),
        indexes = {
                @Index(name = "idx_amistad_solicitante", columnList = "solicitante_id,estado"),
                @Index(name = "idx_amistad_destinatario", columnList = "destinatario_id,estado")
        }
)
@Getter
@Setter
public class Amistad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pair_key", nullable = false, length = 50)
    private String pairKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private User solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private User destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAmistad estado = EstadoAmistad.PENDIENTE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime respondedAt;
}
