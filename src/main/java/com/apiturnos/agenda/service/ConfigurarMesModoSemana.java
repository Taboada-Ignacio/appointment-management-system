package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.DiaAgendaNoValidoException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigurarMesModoSemana {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ConfigurarMesAgenda configurarMesAgenda;
    private final RegistradorAuditoria registradorAuditoria;

    public ConfigurarMesModoSemana(MesAgendaRepository mesAgendaRepository,
                                  DiaAgendaRepository diaAgendaRepository,
                                  BrechaHorariaRepository brechaHorariaRepository,
                                  ConfigurarMesAgenda configurarMesAgenda,
                                  RegistradorAuditoria registradorAuditoria) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.configurarMesAgenda = configurarMesAgenda;
        this.registradorAuditoria = registradorAuditoria;
    }

    public static class DiaSemanaTemplate {
        private final DayOfWeek diaSemana;
        private final List<ConfigurarDiaAgenda.BrechaInput> brechas;

        public DiaSemanaTemplate(DayOfWeek diaSemana, List<ConfigurarDiaAgenda.BrechaInput> brechas) {
            this.diaSemana = diaSemana;
            this.brechas = brechas;
        }

        public DayOfWeek getDiaSemana() {
            return diaSemana;
        }

        public List<ConfigurarDiaAgenda.BrechaInput> getBrechas() {
            return brechas;
        }
    }

    @Transactional
    public MesAgenda ejecutar(Long profesionalId, Long mesAgendaId, List<DiaSemanaTemplate> templates, String usuario) {
        MesAgenda mesAgenda = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (!mesAgenda.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        // Validar brechas de la plantilla
        if (templates != null) {
            for (DiaSemanaTemplate t : templates) {
                if (t.getBrechas() != null) {
                    for (ConfigurarDiaAgenda.BrechaInput b : t.getBrechas()) {
                        if (!b.getHoraInicio().isBefore(b.getHoraFin())) {
                            throw new DiaAgendaNoValidoException(
                                    "La hora de inicio (" + b.getHoraInicio() + ") debe ser anterior a la hora de fin (" + b.getHoraFin() + ")");
                        }
                    }
                }
            }
        }

        // Asegurar que existan todos los DiaAgenda para el mes
        configurarMesAgenda.ejecutar(mesAgendaId);

        List<DiaAgenda> dias = diaAgendaRepository.findByMesAgendaId(mesAgendaId);
        Map<DayOfWeek, List<ConfigurarDiaAgenda.BrechaInput>> templateMap = new HashMap<>();
        if (templates != null) {
            for (DiaSemanaTemplate t : templates) {
                templateMap.put(t.getDiaSemana(), t.getBrechas());
            }
        }

        for (DiaAgenda dia : dias) {
            DayOfWeek dow = dia.getFecha().getDayOfWeek();
            // Limpiar brechas anteriores
            List<BrechaHoraria> existentes = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
            brechaHorariaRepository.deleteAll(existentes);

            List<ConfigurarDiaAgenda.BrechaInput> templateBrechas = templateMap.get(dow);
            if (templateBrechas != null && !templateBrechas.isEmpty()) {
                for (ConfigurarDiaAgenda.BrechaInput b : templateBrechas) {
                    BrechaHoraria nueva = new BrechaHoraria();
                    nueva.setDiaAgenda(dia);
                    nueva.setHoraInicioAtencion(b.getHoraInicio());
                    nueva.setHoraFinAtencion(b.getHoraFin());
                    brechaHorariaRepository.save(nueva);
                }
            }
        }

        registradorAuditoria.registrar("AGENDA", "MesAgenda", mesAgendaId,
                OperacionAuditoria.UPDATE, usuario, profesionalId,
                "Mes configurado en modo SEMANA con " + (templates != null ? templates.size() : 0) + " plantillas");

        return mesAgenda;
    }
}

