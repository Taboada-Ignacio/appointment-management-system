package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.AgendaAnualDuplicadaException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearAgendaAnual {

    private final AgendaAnualRepository agendaAnualRepository;
    private final MesAgendaRepository mesAgendaRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public CrearAgendaAnual(AgendaAnualRepository agendaAnualRepository,
                            MesAgendaRepository mesAgendaRepository,
                            ProfesionalRepository profesionalRepository,
                            RegistradorAuditoria registradorAuditoria) {
        this.agendaAnualRepository = agendaAnualRepository;
        this.mesAgendaRepository = mesAgendaRepository;
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public AgendaAnual ejecutar(Long profesionalId, Integer anio, String usuario) {
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));

        if (agendaAnualRepository.findByProfesionalIdAndAnio(profesionalId, anio).isPresent()) {
            throw new AgendaAnualDuplicadaException(profesionalId, anio);
        }

        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesional);
        agenda.setAnio(anio);
        agenda = agendaAnualRepository.save(agenda);

        for (int mes = 1; mes <= 12; mes++) {
            MesAgenda mesAgenda = new MesAgenda();
            mesAgenda.setAgendaAnual(agenda);
            mesAgenda.setNroMes(mes);
            mesAgenda.setRepetirConfiguracion(false);
            mesAgendaRepository.save(mesAgenda);
        }

        registradorAuditoria.registrar("AGENDA", "AgendaAnual", agenda.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalId,
                "Agenda anual creada para año " + anio + " con 12 meses");

        return agenda;
    }
}
