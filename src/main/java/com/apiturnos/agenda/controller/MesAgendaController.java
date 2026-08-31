package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.*;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.service.*;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/meses-agenda")
@Transactional(readOnly = true)
public class MesAgendaController {

    private final MesAgendaRepository mesAgendaRepository;
    private final AgendaAnualRepository agendaAnualRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final GestorCambioEstado gestorCambioEstado;

    private final ConfigurarMesAgenda configurarMesAgenda;
    private final ConfigurarMesModoSemana configurarMesModoSemana;
    private final ConfigurarMesModoMes configurarMesModoMes;
    private final ActivarInactivarMesAgenda activarInactivarMesAgenda;
    private final ActualizarRepetirConfiguracionMes actualizarRepetirConfiguracionMes;
    private final RepetirConfiguracionMes repetirConfiguracionMes;

    public MesAgendaController(MesAgendaRepository mesAgendaRepository,
                               AgendaAnualRepository agendaAnualRepository,
                               DiaAgendaRepository diaAgendaRepository,
                               BrechaHorariaRepository brechaHorariaRepository,
                               GestorCambioEstado gestorCambioEstado,
                               ConfigurarMesAgenda configurarMesAgenda,
                               ConfigurarMesModoSemana configurarMesModoSemana,
                               ConfigurarMesModoMes configurarMesModoMes,
                               ActivarInactivarMesAgenda activarInactivarMesAgenda,
                               ActualizarRepetirConfiguracionMes actualizarRepetirConfiguracionMes,
                               RepetirConfiguracionMes repetirConfiguracionMes) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.agendaAnualRepository = agendaAnualRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.configurarMesAgenda = configurarMesAgenda;
        this.configurarMesModoSemana = configurarMesModoSemana;
        this.configurarMesModoMes = configurarMesModoMes;
        this.activarInactivarMesAgenda = activarInactivarMesAgenda;
        this.actualizarRepetirConfiguracionMes = actualizarRepetirConfiguracionMes;
        this.repetirConfiguracionMes = repetirConfiguracionMes;
    }

    @GetMapping("/{mesAgendaId}")
    public ResponseEntity<MesAgendaDetalleResponseDto> obtenerDetalle(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId) {
        MesAgenda mes = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (!mes.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        return ResponseEntity.ok(construirDetalleDto(mes));
    }

    @PostMapping("/{mesAgendaId}/dias")
    @Transactional
    public ResponseEntity<List<DiaAgendaResumenResponseDto>> generarDias(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId) {
        MesAgenda mes = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (!mes.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        configurarMesAgenda.ejecutar(mesAgendaId);
        return ResponseEntity.ok(construirDetalleDto(mes).getDias());
    }

    @PostMapping("/{mesAgendaId}/modo-semana")
    @Transactional
    public ResponseEntity<MesAgendaDetalleResponseDto> configurarModoSemana(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId,
            @Valid @RequestBody ConfigurarModoSemanaRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {

        List<ConfigurarMesModoSemana.DiaSemanaTemplate> templates = new ArrayList<>();
        if (request.getDiasSemana() != null) {
            for (DiaSemanaConfiguracionDto d : request.getDiasSemana()) {
                List<ConfigurarDiaAgenda.BrechaInput> brechas = new ArrayList<>();
                if (d.getBrechas() != null) {
                    for (BrechaHorariaRequestDto b : d.getBrechas()) {
                        brechas.add(new ConfigurarDiaAgenda.BrechaInput(b.getHoraInicio(), b.getHoraFin()));
                    }
                }
                templates.add(new ConfigurarMesModoSemana.DiaSemanaTemplate(d.getDiaSemana(), brechas));
            }
        }

        MesAgenda mes = configurarMesModoSemana.ejecutar(profesionalId, mesAgendaId, templates, usuario);
        return ResponseEntity.ok(construirDetalleDto(mes));
    }

    @PostMapping("/{mesAgendaId}/modo-mes")
    @Transactional
    public ResponseEntity<MesAgendaDetalleResponseDto> configurarModoMes(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId,
            @Valid @RequestBody ConfigurarModoMesRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {

        List<ConfigurarMesModoMes.DiaMesTemplate> templates = new ArrayList<>();
        if (request.getDias() != null) {
            for (DiaMesConfiguracionDto d : request.getDias()) {
                List<ConfigurarDiaAgenda.BrechaInput> brechas = new ArrayList<>();
                if (d.getBrechas() != null) {
                    for (BrechaHorariaRequestDto b : d.getBrechas()) {
                        brechas.add(new ConfigurarDiaAgenda.BrechaInput(b.getHoraInicio(), b.getHoraFin()));
                    }
                }
                templates.add(new ConfigurarMesModoMes.DiaMesTemplate(d.getFecha(), brechas));
            }
        }

        MesAgenda mes = configurarMesModoMes.ejecutar(profesionalId, mesAgendaId, templates, usuario);
        return ResponseEntity.ok(construirDetalleDto(mes));
    }

    @PostMapping("/{mesAgendaId}/activar")
    @Transactional
    public ResponseEntity<Void> activar(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        activarInactivarMesAgenda.activar(profesionalId, mesAgendaId, usuario);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{mesAgendaId}/inactivar")
    @Transactional
    public ResponseEntity<Void> inactivar(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        activarInactivarMesAgenda.inactivar(profesionalId, mesAgendaId, usuario);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{mesAgendaId}/repetir-configuracion")
    @Transactional
    public ResponseEntity<MesAgendaResumenResponseDto> actualizarRepetirConfiguracion(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId,
            @Valid @RequestBody ActualizarRepetirConfiguracionRequestDto request) {
        MesAgenda mes = actualizarRepetirConfiguracionMes.ejecutar(
                profesionalId, mesAgendaId, request.getRepetirConfiguracion());
        String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mes.getId());
        return ResponseEntity.ok(new MesAgendaResumenResponseDto(mes, estado));
    }

    @PostMapping("/{mesAgendaId}/repetir")
    @Transactional
    public ResponseEntity<MesAgendaResumenResponseDto> repetirConfiguracionAlSiguienteMes(
            @PathVariable Long profesionalId,
            @PathVariable Long mesAgendaId) {
        MesAgenda mesDestino = repetirConfiguracionMes.ejecutar(profesionalId, mesAgendaId);
        String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mesDestino.getId());
        return ResponseEntity.ok(new MesAgendaResumenResponseDto(mesDestino, estado));
    }

    private MesAgendaDetalleResponseDto construirDetalleDto(MesAgenda mes) {
        String estadoMes = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mes.getId());
        List<DiaAgenda> dias = diaAgendaRepository.findByMesAgendaId(mes.getId());
        List<Long> diaIds = dias.stream().map(DiaAgenda::getId).toList();

        Map<Long, String> estadosDiasMap = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.DIA_AGENDA, diaIds);

        List<DiaAgendaResumenResponseDto> diasDto = dias.stream()
                .sorted(Comparator.comparing(DiaAgenda::getFecha))
                .map(dia -> {
                    int cantBrechas = brechaHorariaRepository.findByDiaAgendaId(dia.getId()).size();
                    return new DiaAgendaResumenResponseDto(dia, cantBrechas, estadosDiasMap.get(dia.getId()));
                })
                .toList();

        return new MesAgendaDetalleResponseDto(mes, estadoMes, diasDto);
    }
}

