package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.DiaAgendaResumenResponseDto;
import com.apiturnos.agenda.dto.MesAgendaDetalleResponseDto;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ObtenerDetalleMesAgenda {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public ObtenerDetalleMesAgenda(MesAgendaRepository mesAgendaRepository,
                                  DiaAgendaRepository diaAgendaRepository,
                                  BrechaHorariaRepository brechaHorariaRepository,
                                  GestorCambioEstado gestorCambioEstado) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @Transactional(readOnly = true)
    public MesAgendaDetalleResponseDto ejecutar(Long profesionalId, Long mesAgendaId) {
        MesAgenda mes = mesAgendaRepository.findByIdWithAgendaAndProfesional(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (!mes.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

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

