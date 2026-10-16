package com.canchasya.api.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="oauth_estados") @Getter @Setter
public class OAuthEstado {
 @Id @Column(length=64) private String hash;
 @Column(nullable=false) private Long duenoId;
 @Column(nullable=false,length=64) private String browserHash;
 @Column(nullable=false,length=500) private String verifier;
 @Column(nullable=false) private Instant expiresAt;
}
