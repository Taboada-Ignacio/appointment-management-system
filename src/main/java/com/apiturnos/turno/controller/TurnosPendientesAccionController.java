package com.apiturnos.turno.controller;

import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.TurnoNoPerteneceProfesionalException;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.service.AprobarTurno;
import com.apiturnos.turno.service.DarDeBajaTurno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/turnos/pendientes-verificacion")
public class TurnosPendientesAccionController {
    private final TurnoRepository turnos;
    private final DiaAgendaRepository dias;
    private final GestorCambioEstado estados;
    private final AprobarTurno aprobar;
    private final DarDeBajaTurno baja;
    public TurnosPendientesAccionController(TurnoRepository turnos, DiaAgendaRepository dias,
            GestorCambioEstado estados, AprobarTurno aprobar, DarDeBajaTurno baja) {
        this.turnos=turnos; this.dias=dias; this.estados=estados; this.aprobar=aprobar; this.baja=baja;
    }
    public record Seleccion(@NotEmpty List<@NotNull @Positive Long> turnoIds) {}
    public record Baja(@NotEmpty List<@NotNull @Positive Long> turnoIds, @NotBlank @Size(max=255) String motivo) {}

    @GetMapping("/ids")
    @Transactional(readOnly=true)
    public List<Long> ids(@PathVariable Long profesionalId) {
        return turnos.findPendientesPorProfesional(profesionalId,Pageable.unpaged()).stream().map(t -> t.getId()).toList();
    }
    @PostMapping("/verificar")
    @Transactional
    public void verificar(@PathVariable Long profesionalId, @Valid @RequestBody Seleccion request,
            @RequestHeader(value="X-Usuario",defaultValue="profesional") String usuario) {
        validar(profesionalId,request.turnoIds());
        request.turnoIds().stream().distinct().sorted().forEach(id -> aprobar.ejecutar(id,usuario));
    }
    @PostMapping("/baja")
    @Transactional
    public void darDeBaja(@PathVariable Long profesionalId, @Valid @RequestBody Baja request,
            @RequestHeader(value="X-Usuario",defaultValue="profesional") String usuario) {
        validar(profesionalId,request.turnoIds());
        request.turnoIds().stream().distinct().sorted().forEach(id -> baja.ejecutar(profesionalId,id,request.motivo(),usuario));
    }
    private void validar(Long profesional, List<Long> ids) {
        var registros=ids.stream().distinct().sorted().map(id -> {
            var t=turnos.findByIdConRelaciones(id).orElseThrow(() -> new NegocioException("Uno de los turnos ya no existe"));
            if(!t.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId().equals(profesional))
                throw new TurnoNoPerteneceProfesionalException(id,profesional);
            return t;
        }).toList();
        registros.stream().map(t -> t.getDiaAgenda().getId()).distinct().sorted().forEach(id -> dias.findByIdForUpdate(id));
        for(var t:registros) {
            turnos.findByIdForUpdate(t.getId());
            if(!"PENDIENTE_DE_APROBACION".equals(estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,t.getId())))
                throw new NegocioException("Uno de los turnos dejó de estar pendiente. Actualizá la lista y seleccioná nuevamente.");
        }
    }
}
