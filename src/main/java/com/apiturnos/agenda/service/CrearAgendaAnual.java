package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.AgendaAnualDuplicadaException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;

@Service
public class CrearAgendaAnual {

    private final AgendaAnualRepository agendaAnualRepository;
    private final MesAgendaRepository mesAgendaRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;
    private final ConfigurarMesAgenda configurarMesAgenda;
    private final GestorCambioEstado gestorCambioEstado;
    private final Clock clock;

    public CrearAgendaAnual(AgendaAnualRepository agendaAnualRepository,
                            MesAgendaRepository mesAgendaRepository,
                            ProfesionalRepository profesionalRepository,
                            RegistradorAuditoria registradorAuditoria,
                            ConfigurarMesAgenda configurarMesAgenda,
                            GestorCambioEstado gestorCambioEstado,
                            Clock clock) {
        this.agendaAnualRepository = agendaAnualRepository;
        this.mesAgendaRepository = mesAgendaRepository;
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
        this.configurarMesAgenda = configurarMesAgenda;
        this.gestorCambioEstado = gestorCambioEstado;
        this.clock = clock;
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

        YearMonth mesActual = YearMonth.now(clock);
        YearMonth mesSiguiente = mesActual.plusMonths(1);

        for (int mes = 1; mes <= 12; mes++) {
            MesAgenda mesAgenda = new MesAgenda();
            mesAgenda.setAgendaAnual(agenda);
            mesAgenda.setNroMes(mes);
            mesAgenda.setRepetirConfiguracion(false);
            mesAgenda = mesAgendaRepository.save(mesAgenda);

            YearMonth mesCreado = YearMonth.of(anio, mes);
            String estadoInicial = mesCreado.equals(mesActual) || mesCreado.equals(mesSiguiente)
                    ? "ACTIVO"
                    : "INACTIVO";
            gestorCambioEstado.registrarCambioInicial(
                    AmbitoEstado.MES_AGENDA, mesAgenda.getId(), estadoInicial, usuario,
                    "Estado inicial al crear agenda anual");
            configurarMesAgenda.ejecutar(mesAgenda.getId(), usuario);
        }

        registradorAuditoria.registrar("AGENDA", "AgendaAnual", agenda.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalId,
                "Agenda anual creada para año " + anio + " con 12 meses");

        return agenda;
    }
}
