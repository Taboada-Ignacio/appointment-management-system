package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.AgendaAnualResponseDto;
import com.apiturnos.agenda.dto.CrearAgendaAnualRequestDto;
import com.apiturnos.agenda.dto.MesAgendaResumenResponseDto;
import com.apiturnos.agenda.dto.InicializarCalendarioRequestDto;
import com.apiturnos.agenda.dto.InicializarCalendarioResponseDto;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.CrearAgendaAnual;
import com.apiturnos.agenda.service.EliminarAgendaAnual;
import com.apiturnos.agenda.service.InicializarCalendarioProfesional;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/agendas")
@Transactional(readOnly = true)
@Tag(name = "Agendas Anuales", description = "Gestión y eliminación en cascada de agendas anuales del profesional")
public class AgendaAnualController {

    private final CrearAgendaAnual crearAgendaAnual;
    private final AgendaAnualRepository agendaAnualRepository;
    private final MesAgendaRepository mesAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final EliminarAgendaAnual eliminarAgendaAnual;
    private final InicializarCalendarioProfesional inicializarCalendarioProfesional;

    public AgendaAnualController(CrearAgendaAnual crearAgendaAnual,
                                 AgendaAnualRepository agendaAnualRepository,
                                 MesAgendaRepository mesAgendaRepository,
                                 GestorCambioEstado gestorCambioEstado,
                                 EliminarAgendaAnual eliminarAgendaAnual,
                                 InicializarCalendarioProfesional inicializarCalendarioProfesional) {
        this.crearAgendaAnual = crearAgendaAnual;
        this.agendaAnualRepository = agendaAnualRepository;
        this.mesAgendaRepository = mesAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.eliminarAgendaAnual = eliminarAgendaAnual;
        this.inicializarCalendarioProfesional = inicializarCalendarioProfesional;
    }

    @PostMapping("/inicializacion")
    @Transactional
    @Operation(summary = "Inicializar calendario del tutorial",
            description = "Crea o reutiliza las agendas necesarias y configura atómicamente el mes actual y, opcionalmente, el siguiente")
    public ResponseEntity<InicializarCalendarioResponseDto> inicializarCalendario(
            @PathVariable Long profesionalId,
            @Valid @RequestBody InicializarCalendarioRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        return ResponseEntity.ok(inicializarCalendarioProfesional.ejecutar(
                profesionalId,
                request.getDiasSemana(),
                !Boolean.FALSE.equals(request.getRepetirAlMesSiguiente()),
                usuario));
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Crear agenda anual", description = "Crea la agenda anual y genera automáticamente los 12 meses")
    public ResponseEntity<AgendaAnualResponseDto> crear(
            @PathVariable Long profesionalId,
            @Valid @RequestBody CrearAgendaAnualRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(profesionalId, request.getAnio(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AgendaAnualResponseDto(agenda));
    }

    @GetMapping
    @Operation(summary = "Listar agendas anuales por profesional")
    public ResponseEntity<List<AgendaAnualResponseDto>> listarPorProfesional(
            @PathVariable Long profesionalId) {
        List<AgendaAnual> agendas = agendaAnualRepository.findByProfesionalId(profesionalId);
        List<AgendaAnualResponseDto> dtos = agendas.stream()
                .map(AgendaAnualResponseDto::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{anio}")
    @Operation(summary = "Obtener agenda anual por año")
    public ResponseEntity<AgendaAnualResponseDto> obtenerPorAnio(
            @PathVariable Long profesionalId,
            @PathVariable Integer anio) {
        AgendaAnual agenda = agendaAnualRepository.findByProfesionalIdAndAnio(profesionalId, anio)
                .orElseThrow(() -> new EntidadNoEncontradaException("AgendaAnual para año " + anio + " del profesional " + profesionalId + " no encontrada"));
        return ResponseEntity.ok(new AgendaAnualResponseDto(agenda));
    }

    @GetMapping("/{anio}/meses")
    @Operation(summary = "Listar meses de una agenda anual")
    public ResponseEntity<List<MesAgendaResumenResponseDto>> listarMeses(
            @PathVariable Long profesionalId,
            @PathVariable Integer anio) {
        AgendaAnual agenda = agendaAnualRepository.findByProfesionalIdAndAnio(profesionalId, anio)
                .orElseThrow(() -> new EntidadNoEncontradaException("AgendaAnual para año " + anio + " del profesional " + profesionalId + " no encontrada"));

        List<MesAgenda> meses = mesAgendaRepository.findByAgendaAnualId(agenda.getId());
        List<Long> mesIds = meses.stream().map(MesAgenda::getId).toList();
        Map<Long, String> estadosMap = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.MES_AGENDA, mesIds);

        List<MesAgendaResumenResponseDto> dtos = meses.stream()
                .sorted(Comparator.comparingInt(MesAgenda::getNroMes))
                .map(mes -> new MesAgendaResumenResponseDto(mes, estadosMap.get(mes.getId())))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/actual")
    @Transactional
    @Operation(summary = "Eliminar la agenda del año actual en cascada", description = "Elimina la agenda del año actual del profesional, borrando en cascada turnos, historiales, brechas horarias, días y meses")
    public ResponseEntity<Void> eliminarAnioActual(
            @PathVariable Long profesionalId,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        eliminarAgendaAnual.ejecutarAnioActual(profesionalId, usuario);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{anio}")
    @Transactional
    @Operation(summary = "Eliminar la agenda de un año específico en cascada", description = "Elimina la agenda del año indicado del profesional, borrando en cascada turnos, historiales, brechas horarias, días y meses")
    public ResponseEntity<Void> eliminarPorAnio(
            @PathVariable Long profesionalId,
            @PathVariable Integer anio,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        eliminarAgendaAnual.ejecutar(profesionalId, anio, usuario);
        return ResponseEntity.noContent().build();
    }
}
