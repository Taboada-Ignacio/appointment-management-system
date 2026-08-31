package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.BrechaHorariaRequestDto;
import com.apiturnos.agenda.dto.BrechaHorariaResponseDto;
import com.apiturnos.agenda.dto.ConfigurarDiaRequestDto;
import com.apiturnos.agenda.dto.DiaAgendaDetalleResponseDto;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.dto.DiaSeleccionableResponseDto;
import com.apiturnos.agenda.service.ConfigurarDiaAgenda;
import com.apiturnos.agenda.service.ObtenerDiasSeleccionables;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/dias-agenda")
@Transactional(readOnly = true)
public class DiaAgendaController {

    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ConfigurarDiaAgenda configurarDiaAgenda;
    private final GestorCambioEstado gestorCambioEstado;
    private final ObtenerDiasSeleccionables obtenerDiasSeleccionables;

    public DiaAgendaController(DiaAgendaRepository diaAgendaRepository,
                               BrechaHorariaRepository brechaHorariaRepository,
                               ConfigurarDiaAgenda configurarDiaAgenda,
                               GestorCambioEstado gestorCambioEstado,
                               ObtenerDiasSeleccionables obtenerDiasSeleccionables) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.configurarDiaAgenda = configurarDiaAgenda;
        this.gestorCambioEstado = gestorCambioEstado;
        this.obtenerDiasSeleccionables = obtenerDiasSeleccionables;
    }

    @GetMapping("/seleccionables")
    public ResponseEntity<List<DiaSeleccionableResponseDto>> listarSeleccionables(
            @PathVariable Long profesionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<DiaSeleccionableResponseDto> dias = obtenerDiasSeleccionables.ejecutar(profesionalId, desde, hasta);
        return ResponseEntity.ok(dias);
    }

    @GetMapping("/{diaAgendaId}")
    public ResponseEntity<DiaAgendaDetalleResponseDto> obtenerDetalle(
            @PathVariable Long profesionalId,
            @PathVariable Long diaAgendaId) {
        DiaAgenda dia = diaAgendaRepository.findById(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));

        if (!dia.getMesAgenda().getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(diaAgendaId, profesionalId);
        }

        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, diaAgendaId);
        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(diaAgendaId);
        List<BrechaHorariaResponseDto> brechasDto = brechas.stream()
                .map(BrechaHorariaResponseDto::new)
                .toList();

        return ResponseEntity.ok(new DiaAgendaDetalleResponseDto(dia, estadoDia, brechasDto));
    }

    @PutMapping("/{diaAgendaId}/brechas")
    @Transactional
    public ResponseEntity<DiaAgendaDetalleResponseDto> configurarBrechas(
            @PathVariable Long profesionalId,
            @PathVariable Long diaAgendaId,
            @Valid @RequestBody ConfigurarDiaRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {

        List<ConfigurarDiaAgenda.BrechaInput> brechasInput = new ArrayList<>();
        if (request.getBrechas() != null) {
            for (BrechaHorariaRequestDto b : request.getBrechas()) {
                brechasInput.add(new ConfigurarDiaAgenda.BrechaInput(b.getHoraInicio(), b.getHoraFin()));
            }
        }

        List<BrechaHoraria> guardadas = configurarDiaAgenda.ejecutar(profesionalId, diaAgendaId, brechasInput, usuario);
        DiaAgenda dia = diaAgendaRepository.findById(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));
        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, diaAgendaId);

        List<BrechaHorariaResponseDto> brechasDto = guardadas.stream()
                .map(BrechaHorariaResponseDto::new)
                .toList();

        return ResponseEntity.ok(new DiaAgendaDetalleResponseDto(dia, estadoDia, brechasDto));
    }
}

