package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.AfectacionTurnoResponseDto;
import com.apiturnos.agenda.model.AfectacionTurnoExcepcion;
import com.apiturnos.agenda.model.EstadoResolucionAfectacion;
import com.apiturnos.agenda.repository.AfectacionTurnoExcepcionRepository;
import com.apiturnos.agenda.dto.BajaMasivaAfectacionesRequestDto;
import com.apiturnos.agenda.dto.ResolverAfectacionRequestDto;
import com.apiturnos.agenda.service.ResolverAfectacionTurno;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/turnos-afectados")
@Tag(name = "Turnos afectados", description = "Seguimiento de turnos afectados por excepciones de agenda")
@Transactional(readOnly = true)
public class AfectacionTurnoExcepcionController {
    private final AfectacionTurnoExcepcionRepository repository;
    private final GestorCambioEstado gestorEstados;
    private final ResolverAfectacionTurno resolver;

    public AfectacionTurnoExcepcionController(
            AfectacionTurnoExcepcionRepository repository,
            GestorCambioEstado gestorEstados,
            ResolverAfectacionTurno resolver) {
        this.repository = repository;
        this.gestorEstados = gestorEstados;
        this.resolver = resolver;
    }

    @PostMapping("/{afectacionId}/baja")
    @Transactional
    public ResponseEntity<AfectacionTurnoResponseDto> darDeBaja(
            @PathVariable Long profesionalId,
            @PathVariable Long afectacionId,
            @RequestBody(required = false) ResolverAfectacionRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        String observacion = request == null ? null : request.observacion();
        return ResponseEntity.ok(convertir(resolver.darDeBaja(profesionalId, afectacionId, observacion, usuario)));
    }

    @PostMapping("/{afectacionId}/reprogramar")
    @Transactional
    public ResponseEntity<AfectacionTurnoResponseDto> reprogramar(
            @PathVariable Long profesionalId,
            @PathVariable Long afectacionId,
            @RequestBody ResolverAfectacionRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        return ResponseEntity.ok(convertir(resolver.reprogramar(profesionalId, afectacionId,
                request.nuevoDiaAgendaId(), request.nuevoInicio(), request.nuevoFin(),
                request.observacion(), usuario)));
    }

    @PostMapping("/baja-masiva")
    @Transactional
    public ResponseEntity<List<AfectacionTurnoResponseDto>> bajaMasiva(
            @PathVariable Long profesionalId,
            @RequestBody BajaMasivaAfectacionesRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        if (request.afectacionIds() == null || request.afectacionIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un turno afectado");
        }
        return ResponseEntity.ok(request.afectacionIds().stream()
                .distinct()
                .map(id -> convertir(resolver.darDeBaja(profesionalId, id, request.observacion(), usuario)))
                .toList());
    }

    @GetMapping
    @Operation(summary = "Listar turnos afectados", description = "Agrupables por excepción; admite filtro por resolución")
    public ResponseEntity<List<AfectacionTurnoResponseDto>> listar(
            @PathVariable Long profesionalId,
            @RequestParam(required = false) EstadoResolucionAfectacion estado) {
        return ResponseEntity.ok(repository.listar(profesionalId, estado).stream().map(this::convertir).toList());
    }

    private AfectacionTurnoResponseDto convertir(AfectacionTurnoExcepcion a) {
        var e = a.getExcepcionAgenda();
        var t = a.getTurno();
        var c = t.getCliente();
        return new AfectacionTurnoResponseDto(
                a.getId(), e.getId(), e.getTipo(), e.getMotivo(), e.isActiva(),
                t.getId(), a.getEstadoResolucion(),
                a.getEstadoTurnoAnterior(),
                gestorEstados.obtenerNombreEstadoActual(AmbitoEstado.TURNO, t.getId()),
                c.getId(), c.getNombre() + " " + c.getApellido(), c.getTelefono(),
                a.getDiaAgendaAnterior().getFecha(), a.getInicioAnterior(), a.getFinAnterior(),
                t.getDiaAgenda().getFecha(), t.getInicioEstimado(), t.getFinEstimado(), a.getObservacion(),
                a.getCreadoEn(), a.getResueltoEn());
    }
}
