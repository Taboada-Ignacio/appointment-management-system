package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.MesAgendaResumenResponseDto;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ListarMesesAgendaAnual {

    private final AgendaAnualRepository agendaAnualRepository;
    private final MesAgendaRepository mesAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public ListarMesesAgendaAnual(AgendaAnualRepository agendaAnualRepository,
                                  MesAgendaRepository mesAgendaRepository,
                                  GestorCambioEstado gestorCambioEstado) {
        this.agendaAnualRepository = agendaAnualRepository;
        this.mesAgendaRepository = mesAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @Transactional(readOnly = true)
    public List<MesAgendaResumenResponseDto> ejecutar(Long profesionalId, Integer anio) {
        AgendaAnual agenda = agendaAnualRepository.findByProfesionalIdAndAnio(profesionalId, anio)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "AgendaAnual para año " + anio + " del profesional " + profesionalId + " no encontrada"));

        List<MesAgenda> meses = mesAgendaRepository.findByAgendaAnualId(agenda.getId());
        List<Long> mesIds = meses.stream().map(MesAgenda::getId).toList();
        Map<Long, String> estadosMap = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.MES_AGENDA, mesIds);

        return meses.stream()
                .sorted(Comparator.comparingInt(MesAgenda::getNroMes))
                .map(mes -> new MesAgendaResumenResponseDto(mes, estadosMap.get(mes.getId())))
                .toList();
    }
}

