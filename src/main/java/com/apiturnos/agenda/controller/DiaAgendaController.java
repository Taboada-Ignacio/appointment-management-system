package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.BrechaHorariaRequestDto;
import com.apiturnos.agenda.dto.BrechaHorariaResponseDto;
import com.apiturnos.agenda.dto.ConfigurarDiaRequestDto;
import com.apiturnos.agenda.dto.DiaAgendaDetalleResponseDto;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.dto.BrechaExcepcionResponseDto;
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

import com.apiturnos.agenda.model.TipoExcepcion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final com.apiturnos.agenda.service.ActivarInactivarDiaAgenda activarInactivarDiaAgenda;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;

    public DiaAgendaController(DiaAgendaRepository diaAgendaRepository,
                               BrechaHorariaRepository brechaHorariaRepository,
                               ConfigurarDiaAgenda configurarDiaAgenda,
                               GestorCambioEstado gestorCambioEstado,
                               ObtenerDiasSeleccionables obtenerDiasSeleccionables,
                               com.apiturnos.agenda.service.ActivarInactivarDiaAgenda activarInactivarDiaAgenda,
                               ExcepcionAgendaRepository excepcionAgendaRepository) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.configurarDiaAgenda = configurarDiaAgenda;
        this.gestorCambioEstado = gestorCambioEstado;
        this.obtenerDiasSeleccionables = obtenerDiasSeleccionables;
        this.activarInactivarDiaAgenda = activarInactivarDiaAgenda;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
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
        List<BrechaExcepcionResponseDto> modificaciones = obtenerModificaciones(profesionalId, dia.getFecha());
        List<BrechaHorariaResponseDto> brechasDto;
        if (!modificaciones.isEmpty()) {
            brechasDto = new ArrayList<>(modificaciones.stream()
                    .map(m -> new BrechaHorariaResponseDto(null, m.horaInicio(), m.horaFin()))
                    .toList());
        } else {
            List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(diaAgendaId);
            brechasDto = new ArrayList<>(brechas.stream()
                    .map(BrechaHorariaResponseDto::new)
                    .toList());
        }

        List<BrechaExcepcionResponseDto> habilitaciones = obtenerHabilitaciones(profesionalId, dia.getFecha());
        for (BrechaExcepcionResponseDto hab : habilitaciones) {
            brechasDto.add(new BrechaHorariaResponseDto(null, hab.horaInicio(), hab.horaFin()));
        }
        brechasDto.sort(Comparator.comparing(BrechaHorariaResponseDto::getHoraInicio));

        List<BrechaExcepcionResponseDto> bloqueos = obtenerBloqueos(profesionalId, dia.getFecha());

        return ResponseEntity.ok(new DiaAgendaDetalleResponseDto(
                dia, estadoDia, brechasDto, bloqueos, habilitaciones));
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

        List<BrechaExcepcionResponseDto> modificaciones = obtenerModificaciones(profesionalId, dia.getFecha());
        List<BrechaHorariaResponseDto> brechasDto;
        if (!modificaciones.isEmpty()) {
            brechasDto = new ArrayList<>(modificaciones.stream()
                    .map(m -> new BrechaHorariaResponseDto(null, m.horaInicio(), m.horaFin()))
                    .toList());
        } else {
            brechasDto = new ArrayList<>(guardadas.stream()
                    .map(BrechaHorariaResponseDto::new)
                    .toList());
        }

        List<BrechaExcepcionResponseDto> habilitaciones = obtenerHabilitaciones(profesionalId, dia.getFecha());
        for (BrechaExcepcionResponseDto hab : habilitaciones) {
            brechasDto.add(new BrechaHorariaResponseDto(null, hab.horaInicio(), hab.horaFin()));
        }
        brechasDto.sort(Comparator.comparing(BrechaHorariaResponseDto::getHoraInicio));

        List<BrechaExcepcionResponseDto> bloqueos = obtenerBloqueos(profesionalId, dia.getFecha());

        return ResponseEntity.ok(new DiaAgendaDetalleResponseDto(
                dia, estadoDia, brechasDto, bloqueos, habilitaciones));
    }

    @PostMapping("/{diaAgendaId}/activar")
    @Transactional
    public ResponseEntity<Void> activar(
            @PathVariable Long profesionalId,
            @PathVariable Long diaAgendaId,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        activarInactivarDiaAgenda.activar(profesionalId, diaAgendaId, usuario);
        return ResponseEntity.ok().build();
    }

    private List<BrechaExcepcionResponseDto> obtenerBloqueos(Long profesionalId, LocalDate fecha) {
        if (excepcionAgendaRepository == null) {
            return List.of();
        }
        return excepcionAgendaRepository.findActivasAplicablesAFecha(profesionalId, fecha).stream()
                .filter(excepcion -> excepcion.isActiva() && excepcion.aplicaEn(fecha))
                .filter(excepcion -> excepcion.getTipo().esBloqueoHorario())
                .flatMap(excepcion -> excepcion.obtenerIntervalos().stream())
                .map(BrechaExcepcionResponseDto::from)
                .distinct()
                .toList();
    }

    private List<BrechaExcepcionResponseDto> obtenerHabilitaciones(Long profesionalId, LocalDate fecha) {
        if (excepcionAgendaRepository == null) {
            return List.of();
        }
        return excepcionAgendaRepository.findActivasAplicablesAFecha(profesionalId, fecha).stream()
                .filter(excepcion -> excepcion.isActiva() && excepcion.aplicaEn(fecha))
                .filter(excepcion -> excepcion.getTipo() == TipoExcepcion.HABILITACION_EXTRAORDINARIA)
                .flatMap(excepcion -> excepcion.obtenerIntervalos().stream())
                .map(BrechaExcepcionResponseDto::from)
                .distinct()
                .toList();
    }

    private List<BrechaExcepcionResponseDto> obtenerModificaciones(Long profesionalId, LocalDate fecha) {
        if (excepcionAgendaRepository == null) {
            return List.of();
        }
        return excepcionAgendaRepository.findActivasAplicablesAFecha(profesionalId, fecha).stream()
                .filter(excepcion -> excepcion.isActiva() && excepcion.aplicaEn(fecha))
                .filter(excepcion -> excepcion.getTipo() == TipoExcepcion.MODIFICACION_HORARIO)
                .flatMap(excepcion -> excepcion.obtenerIntervalos().stream())
                .map(BrechaExcepcionResponseDto::from)
                .distinct()
                .toList();
    }

    @PostMapping("/{diaAgendaId}/inactivar")
    @Transactional
    public ResponseEntity<Void> inactivar(
            @PathVariable Long profesionalId,
            @PathVariable Long diaAgendaId,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        activarInactivarDiaAgenda.inactivar(profesionalId, diaAgendaId, usuario);
        return ResponseEntity.ok().build();
    }
}

