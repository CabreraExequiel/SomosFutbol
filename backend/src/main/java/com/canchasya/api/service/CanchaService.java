package com.canchasya.api.service;

import com.canchasya.api.dto.*;
import com.canchasya.api.dto.CanchaResponse;
import com.canchasya.api.dto.HorarioRequest;
import com.canchasya.api.dto.HorarioResponse;
import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Cancha;
import com.canchasya.api.model.Ciudad;
import com.canchasya.api.model.HorarioDisponible;
import com.canchasya.api.model.User;
import com.canchasya.api.model.Role;
import com.canchasya.api.repository.CanchaRepository;
import com.canchasya.api.repository.CiudadRepository;
import com.canchasya.api.repository.HorarioDisponibleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CanchaService {

    private final CanchaRepository canchaRepository;
    private final CiudadRepository ciudadRepository;
    private final HorarioDisponibleRepository horarioRepository;
    private final ArchivoService archivoService;
    private final BitacoraService bitacora;

    public List<CanchaResponse> buscar(Long ciudadId, String tipo) {
        List<Cancha> canchas;
        if (ciudadId != null && tipo != null) {
            canchas = canchaRepository.findByActivaTrueAndCiudadIdAndTipo(ciudadId, tipo);
        } else if (ciudadId != null) {
            canchas = canchaRepository.findByActivaTrueAndCiudadId(ciudadId);
        } else if (tipo != null) {
            canchas = canchaRepository.findByActivaTrueAndTipo(tipo);
        } else {
            canchas = canchaRepository.findByActivaTrue();
        }
        return canchas.stream().map(CanchaResponse::fromEntity).toList();
    }

    public List<DuenoPublicoResponse> duenos(Long ciudadId, String tipo) {
        var filtradas=buscar(ciudadId,tipo);
        var grupos=filtradas.stream().collect(Collectors.groupingBy(CanchaResponse::getDuenoId));
        return grupos.entrySet().stream().map(e -> {
            var lista=e.getValue();var primero=lista.get(0);
            var user=obtenerCancha(primero.getId()).getDueno();
            return new DuenoPublicoResponse(e.getKey(),CanchaResponse.nombreComplejoPublico(user),user.getAvatar(),
                    lista.stream().map(CanchaResponse::getImagen).filter(java.util.Objects::nonNull).findFirst().orElse(null),
                    lista.size(),lista.stream().map(CanchaResponse::getPrecioHora).min(Comparator.naturalOrder()).orElse(null),
                    lista.stream().map(CanchaResponse::getCiudadNombre).distinct().sorted().toList(),
                    lista.stream().map(CanchaResponse::getTipo).filter(java.util.Objects::nonNull).distinct().sorted().toList());
        }).sorted(Comparator.comparing(DuenoPublicoResponse::nombre,String.CASE_INSENSITIVE_ORDER).thenComparing(DuenoPublicoResponse::id)).toList();
    }

    public List<CanchaResponse> porDueno(Long id) {
        return canchaRepository.findByDuenoId(id).stream().filter(Cancha::isActiva)
                .map(CanchaResponse::fromEntity).toList();
    }

    @Transactional
    public CanchaResponse publicar(Long id, boolean activa, User user) {
        var cancha=canchaRepository.findByIdForUpdate(id).orElseThrow(() -> new ApiException("Cancha no encontrada",HttpStatus.NOT_FOUND));
        validarPropietario(cancha,user);cancha.setActiva(activa);return CanchaResponse.fromEntity(canchaRepository.save(cancha));
    }

    public CanchaResponse ver(Long id) {
        Cancha cancha = obtenerCancha(id);
        if (!cancha.isActiva()) throw new ApiException("La cancha está pausada", HttpStatus.NOT_FOUND);
        return CanchaResponse.fromEntity(cancha);
    }

    public List<CanchaResponse> misCanchas(User dueno) {
        return canchaRepository.findByDuenoId(dueno.getId()).stream()
                .map(CanchaResponse::fromEntity)
                .toList();
    }

    public CanchaResponse crear(CanchaRequest request, User dueno) {
        if (dueno.getRole() != Role.DUENO && dueno.getRole() != Role.ADMINISTRADOR) {
            throw new ApiException("Solo los dueños pueden publicar canchas", HttpStatus.FORBIDDEN);
        }

        Ciudad ciudad = ciudadRepository.findById(request.getCiudadId())
                .orElseThrow(() -> new ApiException("Ciudad no encontrada", HttpStatus.NOT_FOUND));

        Cancha cancha = new Cancha();
        cancha.setDueno(dueno);
        cancha.setCiudad(ciudad);
        aplicarDatos(cancha, request);

        canchaRepository.save(cancha);
        bitacora.evento("CANCHA_CREADA", "CANCHA", cancha.getId(), dueno.getId());
        return CanchaResponse.fromEntity(cancha);
    }

    @Transactional
    public CanchaResponse actualizar(Long id, CanchaRequest request, User dueno) {
        Cancha cancha = canchaRepository.findByIdForUpdate(id).orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));
        validarPropietario(cancha, dueno);

        Ciudad ciudad = ciudadRepository.findById(request.getCiudadId())
                .orElseThrow(() -> new ApiException("Ciudad no encontrada", HttpStatus.NOT_FOUND));

        cancha.setCiudad(ciudad);
        aplicarDatos(cancha, request);

        canchaRepository.save(cancha);
        bitacora.evento("CANCHA_EDITADA", "CANCHA", cancha.getId(), dueno.getId());
        return CanchaResponse.fromEntity(cancha);
    }

    @Transactional
    public void eliminar(Long id, User dueno) {
        Cancha cancha = canchaRepository.findByIdForUpdate(id).orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));
        validarPropietario(cancha, dueno);
        cancha.setActiva(false);
        canchaRepository.save(cancha);
        bitacora.evento("CANCHA_DESACTIVADA", "CANCHA", cancha.getId(), dueno.getId());
    }

    @Transactional
    public CanchaResponse actualizarImagen(Long id, MultipartFile archivo, User dueno) {
        Cancha cancha = canchaRepository.findByIdForUpdate(id).orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));
        validarPropietario(cancha, dueno);

        String anterior = cancha.getImagen();
        String url = archivoService.guardarImagen(archivo, "canchas");
        cancha.setImagen(url);
        canchaRepository.save(cancha);
        archivoService.eliminarSiEsLocal(anterior);
        return CanchaResponse.fromEntity(cancha);
    }

    public List<HorarioResponse> horarios(Long canchaId, Integer diaSemana) {
        List<HorarioDisponible> horarios = diaSemana != null
                ? horarioRepository.findByCanchaIdAndDiaSemana(canchaId, diaSemana)
                : horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(canchaId);
        return horarios.stream().map(HorarioResponse::fromEntity).toList();
    }

    @Transactional
    public HorarioResponse agregarHorario(Long canchaId, HorarioRequest request, User dueno) {
        Cancha cancha = canchaRepository.findByIdForUpdate(canchaId).orElseThrow(() -> new ApiException("Cancha no encontrada",HttpStatus.NOT_FOUND));
        validarPropietario(cancha, dueno);
        DisponibilidadService.validarRango(request.getHoraInicio(),request.getHoraFin());
        boolean overlap=horarioRepository.findByCanchaIdAndDiaSemana(canchaId,request.getDiaSemana()).stream()
                .anyMatch(h -> request.getHoraInicio().isBefore(h.getHoraFin()) && request.getHoraFin().isAfter(h.getHoraInicio()));
        if(overlap) throw new ApiException("Ese horario se superpone con otro",HttpStatus.CONFLICT);

        HorarioDisponible horario = new HorarioDisponible();
        horario.setCancha(cancha);
        horario.setDiaSemana(request.getDiaSemana());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());

        horarioRepository.save(horario);
        return HorarioResponse.fromEntity(horario);
    }

    @Transactional
    public void eliminarHorario(Long id, Long horarioId, User user) {
        var cancha=canchaRepository.findByIdForUpdate(id).orElseThrow(() -> new ApiException("Cancha no encontrada",HttpStatus.NOT_FOUND));
        validarPropietario(cancha,user);
        var h=horarioRepository.findById(horarioId).orElseThrow(() -> new ApiException("Horario no encontrado",HttpStatus.NOT_FOUND));
        if(!h.getCancha().getId().equals(id)) throw new ApiException("Horario no encontrado",HttpStatus.NOT_FOUND);
        horarioRepository.delete(h);
    }

    private void aplicarDatos(Cancha cancha, CanchaRequest request) {
        cancha.setNombre(request.getNombre());
        cancha.setDireccion(request.getDireccion());
        cancha.setTipo(request.getTipo());
        cancha.setSuperficie(request.getSuperficie());
        cancha.setCantJugadores(request.getCantJugadores());
        cancha.setPrecioHora(request.getPrecioHora());
        cancha.setTechada(request.isTechada());
        cancha.setDescripcion(request.getDescripcion());
    }

    private Cancha obtenerCancha(Long id) {
        return canchaRepository.findById(id)
                .orElseThrow(() -> new ApiException("Cancha no encontrada", HttpStatus.NOT_FOUND));
    }

    private void validarPropietario(Cancha cancha, User user) {
        if (!cancha.getDueno().getId().equals(user.getId())) {
            throw new ApiException("No tenés permiso sobre esta cancha", HttpStatus.FORBIDDEN);
        }
    }
}
