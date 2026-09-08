package com.apiturnos.turno.controller;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.turno.dto.TurnoResponseDto;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/turnos")
@Transactional(readOnly = true)
public class TurnoConsultaController {

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public TurnoConsultaController(TurnoRepository turnoRepository, GestorCambioEstado gestorCambioEstado) {
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @GetMapping
    public ResponseEntity<List<TurnoResponseDto>> listarAsignados(
            @PathVariable Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        var turnos = turnoRepository.findByProfesionalAndFechaBetween(profesionalId, desde, hasta);
        Map<Long, String> estados = turnos.isEmpty() ? Map.of()
                : gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                        AmbitoEstado.TURNO, turnos.stream().map(turno -> turno.getId()).toList());
        return ResponseEntity.ok(turnos.stream()
                .filter(turno -> "ASIGNADO".equals(estados.get(turno.getId())))
                .map(turno -> TurnoResponseDto.from(turno, "ASIGNADO"))
                .toList());
    }
}
