package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.Publicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.repository.PublicidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;

/** 10A: administración sin exposición pública de anuncios hasta la etapa 10C. */
@Service
@RequiredArgsConstructor
public class PublicidadAdminService {
    private final PublicidadRepository repository;
    private final BitacoraService bitacora;

    public record Solicitud(String titulo, String descripcion, String imagenUrl,
                            String destinoUrl, UbicacionPublicidad ubicacion,
                            Integer prioridad, Instant fechaInicio, Instant fechaFin) {}

    public record CambioEstado(EstadoPublicidad estado) {}

    public record Respuesta(Long id, String titulo, String descripcion, String imagenUrl,
                            String destinoUrl, UbicacionPublicidad ubicacion, EstadoPublicidad estado,
                            int prioridad, Instant fechaInicio, Instant fechaFin,
                            Instant creadoEn, Instant actualizadoEn,
                            Long creadoPorId, Long actualizadoPorId) {
        static Respuesta de(Publicidad p) {
            return new Respuesta(p.getId(), p.getTitulo(), p.getDescripcion(), p.getImagenUrl(),
                    p.getDestinoUrl(), p.getUbicacion(), p.getEstado(), p.getPrioridad(),
                    p.getFechaInicio(), p.getFechaFin(), p.getCreadoEn(), p.getActualizadoEn(),
                    p.getCreadoPorId(), p.getActualizadoPorId());
        }
    }

    @Transactional(readOnly = true)
    public Page<Respuesta> listar(String buscar, EstadoPublicidad estado,
                                 UbicacionPublicidad ubicacion, int pagina) {
        if (pagina < 0 || pagina > 10000)
            throw new ApiException("Página inválida", HttpStatus.BAD_REQUEST);
        String texto = buscar == null ? "" : buscar.strip();
        if (texto.length() > 100)
            throw new ApiException("La búsqueda supera 100 caracteres", HttpStatus.BAD_REQUEST);
        Specification<Publicidad> spec = (root, query, cb) -> cb.conjunction();
        if (!texto.isBlank()) {
            String patron = "%" + escaparLike(texto.toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.like(
                    cb.lower(root.get("titulo")), patron, '\\'));
        }
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (ubicacion != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ubicacion"), ubicacion));
        }
        return repository.findAll(spec, PageRequest.of(pagina, 12,
                Sort.by(Sort.Order.desc("prioridad"), Sort.Order.desc("id"))))
                .map(Respuesta::de);
    }

    private static String escaparLike(String entrada) {
        return entrada.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional(readOnly = true)
    public Respuesta obtener(Long id) {
        return Respuesta.de(buscar(id));
    }

    @Transactional
    public Respuesta crear(Solicitud solicitud, Long adminId) {
        validarActor(adminId);
        Publicidad anuncio = new Publicidad();
        aplicar(anuncio, solicitud);
        Instant ahora = Instant.now();
        anuncio.setEstado(EstadoPublicidad.BORRADOR);
        anuncio.setCreadoEn(ahora);
        anuncio.setActualizadoEn(ahora);
        anuncio.setCreadoPorId(adminId);
        anuncio.setActualizadoPorId(adminId);
        Publicidad guardado = repository.saveAndFlush(anuncio);
        bitacora.evento("PUBLICIDAD_CREADA", "PUBLICIDAD", guardado.getId(), adminId);
        return Respuesta.de(guardado);
    }

    @Transactional
    public Respuesta editar(Long id, Solicitud solicitud, Long adminId) {
        validarActor(adminId);
        Publicidad anuncio = buscar(id);
        if (anuncio.getEstado() == EstadoPublicidad.ARCHIVADA)
            throw new ApiException("No se puede editar una publicidad archivada", HttpStatus.CONFLICT);
        aplicar(anuncio, solicitud);
        anuncio.setActualizadoEn(Instant.now());
        anuncio.setActualizadoPorId(adminId);
        Publicidad guardado = repository.saveAndFlush(anuncio);
        bitacora.evento("PUBLICIDAD_EDITADA", "PUBLICIDAD", guardado.getId(), adminId);
        return Respuesta.de(guardado);
    }

    @Transactional
    public Respuesta cambiarEstado(Long id, CambioEstado solicitud, Long adminId) {
        validarActor(adminId);
        Publicidad anuncio = buscar(id);
        if (solicitud == null || solicitud.estado() == null)
            throw new ApiException("Debés indicar un estado", HttpStatus.BAD_REQUEST);
        if (anuncio.getEstado() == EstadoPublicidad.ARCHIVADA)
            throw new ApiException("Una publicidad archivada no se puede reactivar", HttpStatus.CONFLICT);
        if (anuncio.getEstado() == solicitud.estado()) return Respuesta.de(anuncio);
        anuncio.setEstado(solicitud.estado());
        anuncio.setActualizadoEn(Instant.now());
        anuncio.setActualizadoPorId(adminId);
        Publicidad guardado = repository.saveAndFlush(anuncio);
        bitacora.evento("PUBLICIDAD_" + solicitud.estado().name(), "PUBLICIDAD", guardado.getId(), adminId);
        return Respuesta.de(guardado);
    }

    private Publicidad buscar(Long id) {
        if (id == null || id <= 0)
            throw new ApiException("Identificador inválido", HttpStatus.BAD_REQUEST);
        return repository.findById(id).orElseThrow(() ->
                new ApiException("Publicidad no encontrada", HttpStatus.NOT_FOUND));
    }

    private static void validarActor(Long id) {
        if (id == null || id <= 0)
            throw new ApiException("Administrador inválido", HttpStatus.FORBIDDEN);
    }

    private static void aplicar(Publicidad anuncio, Solicitud r) {
        if (r == null) throw new ApiException("Faltan los datos de la publicidad", HttpStatus.BAD_REQUEST);
        anuncio.setTitulo(textoObligatorio(r.titulo(), 120, "Título"));
        anuncio.setDescripcion(textoOpcional(r.descripcion(), 400, "Descripción"));
        anuncio.setImagenUrl(urlHttps(r.imagenUrl(), false, "Imagen"));
        anuncio.setDestinoUrl(urlHttps(r.destinoUrl(), true, "Destino"));
        if (r.ubicacion() == null)
            throw new ApiException("Debés seleccionar dónde se mostrará", HttpStatus.BAD_REQUEST);
        anuncio.setUbicacion(r.ubicacion());
        int prioridad = r.prioridad() == null ? 0 : r.prioridad();
        if (prioridad < 0 || prioridad > 100)
            throw new ApiException("Prioridad fuera del rango 0–100", HttpStatus.BAD_REQUEST);
        anuncio.setPrioridad(prioridad);
        if (r.fechaInicio() != null && r.fechaFin() != null
                && !r.fechaFin().isAfter(r.fechaInicio()))
            throw new ApiException("La fecha final debe ser posterior a la inicial", HttpStatus.BAD_REQUEST);
        anuncio.setFechaInicio(r.fechaInicio());
        anuncio.setFechaFin(r.fechaFin());
    }

    private static String textoObligatorio(String entrada, int largo, String nombre) {
        String valor = textoOpcional(entrada, largo, nombre);
        if (valor == null) throw new ApiException(nombre + " obligatorio", HttpStatus.BAD_REQUEST);
        return valor;
    }

    private static String textoOpcional(String entrada, int largo, String nombre) {
        if (entrada == null || entrada.isBlank()) return null;
        String valor = entrada.strip();
        if (valor.length() > largo || valor.chars().anyMatch(c -> Character.isISOControl(c)))
            throw new ApiException(nombre + " inválido", HttpStatus.BAD_REQUEST);
        return valor;
    }

    private static String urlHttps(String entrada, boolean requerida, String campo) {
        String valor = textoOpcional(entrada, 700, campo);
        if (valor == null) {
            if (requerida) throw new ApiException("URL de " + campo + " obligatoria", HttpStatus.BAD_REQUEST);
            return null;
        }
        try {
            URI uri = new URI(valor);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getHost().isBlank() || uri.getRawUserInfo() != null
                    || uri.getPort() != -1 && uri.getPort() != 443)
                throw new ApiException("La URL de " + campo + " debe ser HTTPS válida", HttpStatus.BAD_REQUEST);
        } catch (URISyntaxException e) {
            throw new ApiException("La URL de " + campo + " es inválida", HttpStatus.BAD_REQUEST);
        }
        return valor;
    }
}
