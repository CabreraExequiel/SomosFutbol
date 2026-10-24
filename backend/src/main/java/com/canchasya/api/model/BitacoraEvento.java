package com.canchasya.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "bitacora_eventos", indexes = {
        @Index(name = "idx_bitacora_fecha", columnList = "fecha"),
        @Index(name = "idx_bitacora_nivel_fecha", columnList = "nivel,fecha"),
        @Index(name = "idx_bitacora_origen_fecha", columnList = "origen,fecha")
})
@Getter @Setter
public class BitacoraEvento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, length = 16)
    private String nivel;

    @Column(nullable = false, length = 20)
    private String origen;

    @Column(nullable = false, length = 32)
    private String ambiente;

    @Column(nullable = false, length = 64)
    private String tipo;

    @Column(nullable = false, unique = true, length = 48)
    private String codigo;

    @Column(nullable = false, length = 240)
    private String descripcion;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "referencia_tipo", length = 32)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(length = 180)
    private String ruta;

    // Datos estructurados (sin stack traces, SQL, contraseñas ni cuerpos HTTP).
    @Column(name = "clase_origen", length = 180)
    private String claseOrigen;

    @Column(name = "funcion_origen", length = 120)
    private String funcionOrigen;

    @Column(name = "archivo_origen", length = 150)
    private String archivoOrigen;

    @Column(name = "linea_origen")
    private Integer lineaOrigen;

    @Column(name = "causa_probable", length = 240)
    private String causaProbable;

    @Column(name = "sql_state", length = 5)
    private String sqlState;

    @Column(name = "codigo_sql")
    private Integer codigoSql;

    // Gestión administrativa del incidente (sin modificar el diagnóstico original).
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "atendido_por_id")
    private Long atendidoPorId;

    @Column(name = "atendido_en")
    private Instant atendidoEn;
}
