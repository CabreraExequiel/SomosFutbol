package com.canchasya.api.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="mercadopago_cuentas") @Getter @Setter
public class MercadoPagoCuenta {
 @Id private Long duenoId;
 @Column(nullable=false,unique=true,length=80) private String mpUserId;
 @Column(nullable=false,length=3000) private String accessToken;
 @Column(nullable=false,length=3000) private String refreshToken;
 @Column(nullable=false) private Instant expiresAt;
 private boolean liveMode;
}
