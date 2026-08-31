package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.BrechaHorariaResponseDto;
import com.apiturnos.agenda.dto.DiaAgendaDetalleResponseDto;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ObtenerDetalleDiaAgenda {

    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public ObtenerDetalleDiaAgenda(DiaAgendaRepository diaAgendaRepository,
                                  BrechaHorariaRepository brechaHorariaRepository,
                                  GestorCambioEstado gestorCambioEstado) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @Transactional(readOnly = true)
    public DiaAgendaDetalleResponseDto ejecutar(Long profesionalId, Long diaAgendaId) {
        DiaAgenda dia = diaAgendaRepository.findByIdWithMesAndProfesional(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));

        if (!dia.getMesAgenda().getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(diaAgendaId, profesionalId);
        }

        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, diaAgendaId);
        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(diaAgendaId);
        List<BrechaHorariaResponseDto> brechasDto = brechas.stream()
                .map(BrechaHorariaResponseDto::new)
                .toList();

        return new DiaAgendaDetalleResponseDto(dia, estadoDia, brechasDto);
    }
}

