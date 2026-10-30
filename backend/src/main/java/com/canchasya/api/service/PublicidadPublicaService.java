package com.canchasya.api.service;

import com.canchasya.api.model.EstadoPublicidad;
import com.canchasya.api.model.Publicidad;
import com.canchasya.api.model.UbicacionPublicidad;
import com.canchasya.api.repository.PublicidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Etapa 10C: anuncios publicables, sin campos de auditoría ni datos internos. */
@Service
@RequiredArgsConstructor
public class PublicidadPublicaService {
    private final PublicidadRepository repository;

    public record Anuncio(Long id, String titulo, String descripcion, String imagenUrl,
                          String destinoUrl, UbicacionPublicidad ubicacion) {
        static Anuncio desde(Publicidad p) {
            return new Anuncio(p.getId(), p.getTitulo(), p.getDescripcion(),
                    p.getImagenUrl(), p.getDestinoUrl(), p.getUbicacion());
        }
    }

    @Transactional(readOnly = true)
    public List<Anuncio> visibles(UbicacionPublicidad ubicacion) {
        if (ubicacion == null) {
            throw new IllegalArgumentException("Ubicación requerida");
        }
        Instant ahora = Instant.now();
        Specification<Publicidad> filtro = (root, query, cb) -> cb.and(
                cb.equal(root.get("estado"), EstadoPublicidad.ACTIVA),
                cb.equal(root.get("ubicacion"), ubicacion),
                cb.or(cb.isNull(root.get("fechaInicio")),
                        cb.lessThanOrEqualTo(root.<Instant>get("fechaInicio"), ahora)),
                cb.or(cb.isNull(root.get("fechaFin")),
                        cb.greaterThan(root.<Instant>get("fechaFin"), ahora))
        );

        return repository.findAll(filtro, PageRequest.of(0, 2,
                        Sort.by(Sort.Order.desc("prioridad"), Sort.Order.desc("id"))))
                .stream().map(Anuncio::desde).toList();
    }
}
