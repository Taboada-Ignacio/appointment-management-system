package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActualizarRepetirConfiguracionMes {

    private final MesAgendaRepository mesAgendaRepository;

    public ActualizarRepetirConfiguracionMes(MesAgendaRepository mesAgendaRepository) {
        this.mesAgendaRepository = mesAgendaRepository;
    }

    @Transactional
    public MesAgenda ejecutar(Long profesionalId, Long mesAgendaId, Boolean repetirConfiguracion) {
        MesAgenda mes = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (!mes.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        mes.setRepetirConfiguracion(repetirConfiguracion != null ? repetirConfiguracion : false);
        return mesAgendaRepository.save(mes);
    }
}

