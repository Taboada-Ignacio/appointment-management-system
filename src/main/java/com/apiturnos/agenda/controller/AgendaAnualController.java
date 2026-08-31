package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.AgendaAnualResponseDto;
import com.apiturnos.agenda.dto.CrearAgendaAnualRequestDto;
import com.apiturnos.agenda.dto.MesAgendaResumenResponseDto;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.CrearAgendaAnual;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
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
public class AgendaAnualController {

    private final CrearAgendaAnual crearAgendaAnual;
    private final AgendaAnualRepository agendaAnualRepository;
    private final MesAgendaRepository mesAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public AgendaAnualController(CrearAgendaAnual crearAgendaAnual,
                                 AgendaAnualRepository agendaAnualRepository,
                                 MesAgendaRepository mesAgendaRepository,
                                 GestorCambioEstado gestorCambioEstado) {
        this.crearAgendaAnual = crearAgendaAnual;
        this.agendaAnualRepository = agendaAnualRepository;
        this.mesAgendaRepository = mesAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AgendaAnualResponseDto> crear(
            @PathVariable Long profesionalId,
            @Valid @RequestBody CrearAgendaAnualRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        AgendaAnual agenda = crearAgendaAnual.ejecutar(profesionalId, request.getAnio(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AgendaAnualResponseDto(agenda));
    }

    @GetMapping
    public ResponseEntity<List<AgendaAnualResponseDto>> listarPorProfesional(
            @PathVariable Long profesionalId) {
        List<AgendaAnual> agendas = agendaAnualRepository.findByProfesionalId(profesionalId);
        List<AgendaAnualResponseDto> dtos = agendas.stream()
                .map(AgendaAnualResponseDto::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{anio}")
    public ResponseEntity<AgendaAnualResponseDto> obtenerPorAnio(
            @PathVariable Long profesionalId,
            @PathVariable Integer anio) {
        AgendaAnual agenda = agendaAnualRepository.findByProfesionalIdAndAnio(profesionalId, anio)
                .orElseThrow(() -> new EntidadNoEncontradaException("AgendaAnual para año " + anio + " del profesional " + profesionalId + " no encontrada"));
        return ResponseEntity.ok(new AgendaAnualResponseDto(agenda));
    }

    @GetMapping("/{anio}/meses")
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
}

