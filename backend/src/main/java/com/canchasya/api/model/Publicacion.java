package com.canchasya.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "publicaciones")
@Getter
@Setter
public class Publicacion {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 2000)
    private String contenido;

    private String imagen;

    @Column(name = "youtube_video_id", length = 11)
    private String youtubeVideoId;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "video_public_id", length = 255)
    private String videoPublicId;

    @Column(name = "video_thumbnail_url", length = 1000)
    private String videoThumbnailUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "publicacion_menciones",
            joinColumns = @JoinColumn(name = "publicacion_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"publicacion_id", "user_id"})
    )
    private Set<User> mencionados = new HashSet<>();
}
