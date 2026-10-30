package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.Publicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.repository.PublicidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 10D: métricas agregadas. Sin almacenar IP, sesiones ni datos personales. */
@Service
@RequiredArgsConstructor
public class PublicidadMetricasService {
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private final PublicidadRepository publicidadRepository;
    private final NamedParameterJdbcTemplate jdbc;

    public record Totales(long impresiones, long clics, double ctr) {}
    public record Dia(LocalDate fecha, long impresiones, long clics, double ctr) {}
    public record Anuncio(Long id, String titulo, String ubicacion, String estado,
                          long impresiones, long clics, double ctr) {}
    public record Estadisticas(int dias, UbicacionPublicidad ubicacion, Long publicidadId,
                               LocalDate desde, LocalDate hasta, Totales totales,
                               List<Dia> evolucion, List<Anuncio> anuncios) {}

    /** La solicitud pública solo puede contar anuncios realmente publicables. */
    @Transactional
    public void registrar(Long id, UbicacionPublicidad ubicacion, boolean esClic) {
        if (id == null || id <= 0 || ubicacion == null) {
            throw new ApiException("Publicidad o ubicación inválida", HttpStatus.BAD_REQUEST);
        }
        Publicidad publicidad = publicidadRepository.findById(id).orElseThrow(() ->
                new ApiException("Publicidad no disponible", HttpStatus.NOT_FOUND));
        Instant ahora = Instant.now();
        if (publicidad.getEstado() != EstadoPublicidad.ACTIVA
                || publicidad.getUbicacion() != ubicacion
                || publicidad.getFechaInicio() != null && publicidad.getFechaInicio().isAfter(ahora)
                || publicidad.getFechaFin() != null && !publicidad.getFechaFin().isAfter(ahora)) {
            throw new ApiException("Publicidad no disponible", HttpStatus.NOT_FOUND);
        }

        String sql = "INSERT INTO publicidad_metricas_diarias "
                + "(publicidad_id, fecha, ubicacion, impresiones, clics) "
                + "VALUES (:id, :fecha, :ubicacion, :impresiones, :clics) "
                + "ON DUPLICATE KEY UPDATE "
                + (esClic ? "clics = clics + 1" : "impresiones = impresiones + 1");
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("fecha", LocalDate.now(ZONA))
                .addValue("ubicacion", ubicacion.name())
                .addValue("impresiones", esClic ? 0L : 1L)
                .addValue("clics", esClic ? 1L : 0L));
    }

    /** Agregación de solo lectura. Se permiten exactamente 7 o 30 días. */
    @Transactional(readOnly = true)
    public Estadisticas estadisticas(int dias, UbicacionPublicidad ubicacion, Long id) {
        if (dias != 7 && dias != 30) {
            throw new ApiException("El período debe ser de 7 o 30 días", HttpStatus.BAD_REQUEST);
        }
        if (id != null && id <= 0) {
            throw new ApiException("ID de publicidad inválido", HttpStatus.BAD_REQUEST);
        }
        LocalDate hasta = LocalDate.now(ZONA);
        LocalDate desde = hasta.minusDays(dias - 1L);
        var parametros = new MapSqlParameterSource().addValue("desde", desde).addValue("hasta", hasta);
        String condicion = " WHERE m.fecha BETWEEN :desde AND :hasta ";
        if (ubicacion != null) {
            condicion += " AND m.ubicacion = :ubicacion ";
            parametros.addValue("ubicacion", ubicacion.name());
        }
        if (id != null) {
            condicion += " AND m.publicidad_id = :id ";
            parametros.addValue("id", id);
        }

        var porDia = jdbc.query("SELECT m.fecha, SUM(m.impresiones) impresiones, "
                        + "SUM(m.clics) clics FROM publicidad_metricas_diarias m "
                        + condicion + " GROUP BY m.fecha ORDER BY m.fecha ASC", parametros,
                (rs, n) -> new Dia(rs.getDate("fecha").toLocalDate(),
                        rs.getLong("impresiones"), rs.getLong("clics"), 0));
        Map<LocalDate, Dia> mapa = new TreeMap<>();
        porDia.forEach(d -> mapa.put(d.fecha(), d));
        long totalImpresiones = 0, totalClics = 0;
        var evolucion = new java.util.ArrayList<Dia>(dias);
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = desde.plusDays(i);
            Dia d = mapa.getOrDefault(fecha, new Dia(fecha, 0, 0, 0));
            totalImpresiones += d.impresiones();
            totalClics += d.clics();
            evolucion.add(new Dia(fecha, d.impresiones(), d.clics(), ctr(d.impresiones(), d.clics())));
        }
        var anuncios = jdbc.query("SELECT p.id, p.titulo, p.ubicacion, p.estado, "
                        + "SUM(m.impresiones) impresiones, SUM(m.clics) clics "
                        + "FROM publicidad_metricas_diarias m "
                        + "JOIN publicidades p ON p.id = m.publicidad_id "
                        + condicion + " GROUP BY p.id, p.titulo, p.ubicacion, p.estado "
                        + "ORDER BY impresiones DESC, clics DESC, p.id DESC LIMIT 30", parametros,
                (rs, n) -> new Anuncio(rs.getLong("id"), rs.getString("titulo"),
                        rs.getString("ubicacion"), rs.getString("estado"),
                        rs.getLong("impresiones"), rs.getLong("clics"),
                        ctr(rs.getLong("impresiones"), rs.getLong("clics"))));

        return new Estadisticas(dias, ubicacion, id, desde, hasta,
                new Totales(totalImpresiones, totalClics, ctr(totalImpresiones, totalClics)),
                evolucion, anuncios);
    }

    private static double ctr(long impresiones, long clics) {
        return impresiones <= 0 ? 0 : Math.round(clics * 10000.0 / impresiones) / 100.0;
    }
}
