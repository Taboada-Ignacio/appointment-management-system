package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.DiaAgendaNoValidoException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConfigurarDiaAgenda {

    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final RegistradorAuditoria registradorAuditoria;
    private final ProcesarBajasTurnosPorCambioAgenda procesarBajasTurnos;

    public ConfigurarDiaAgenda(DiaAgendaRepository diaAgendaRepository,
                                BrechaHorariaRepository brechaHorariaRepository,
                                RegistradorAuditoria registradorAuditoria,
                                ProcesarBajasTurnosPorCambioAgenda procesarBajasTurnos) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.registradorAuditoria = registradorAuditoria;
        this.procesarBajasTurnos = procesarBajasTurnos;
    }

    public static class BrechaInput {
        private final LocalTime horaInicio;
        private final LocalTime horaFin;

        public BrechaInput(LocalTime horaInicio, LocalTime horaFin) {
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
        }

        public LocalTime getHoraInicio() { return horaInicio; }
        public LocalTime getHoraFin() { return horaFin; }
    }

    @Transactional
    public List<BrechaHoraria> ejecutar(Long profesionalId, Long diaAgendaId, List<BrechaInput> brechas, String usuario) {
        DiaAgenda diaAgenda = diaAgendaRepository.findById(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));

        if (profesionalId != null && !diaAgenda.getMesAgenda().getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(diaAgendaId, profesionalId);
        }

        // Validate each brecha
        if (brechas != null) {
            for (BrechaInput b : brechas) {
                if (!b.getHoraInicio().isBefore(b.getHoraFin())) {
                    throw new DiaAgendaNoValidoException(
                            "La hora de inicio (" + b.getHoraInicio() + ") debe ser anterior a la hora de fin (" + b.getHoraFin() + ")");
                }
            }
        }

        // Delete existing brechas
        List<BrechaHoraria> existentes = brechaHorariaRepository.findByDiaAgendaId(diaAgendaId);
        brechaHorariaRepository.deleteAll(existentes);

        // Create new brechas
        List<BrechaHoraria> nuevas = new ArrayList<>();
        if (brechas != null) {
            for (BrechaInput b : brechas) {
                BrechaHoraria brecha = new BrechaHoraria();
                brecha.setDiaAgenda(diaAgenda);
                brecha.setHoraInicioAtencion(b.getHoraInicio());
                brecha.setHoraFinAtencion(b.getHoraFin());
                nuevas.add(brechaHorariaRepository.save(brecha));
            }
        }

        Long idProf = diaAgenda.getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("AGENDA", "DiaAgenda", diaAgendaId,
                OperacionAuditoria.UPDATE, usuario, idProf,
                "Día configurado con " + nuevas.size() + " brechas horarias");

        procesarBajasTurnos.ejecutar(
                diaAgenda, "nueva configuración horaria del día " + diaAgenda.getFecha(), usuario);

        return nuevas;
    }

    @Transactional
    public List<BrechaHoraria> ejecutar(Long diaAgendaId, List<BrechaInput> brechas, String usuario) {
        return ejecutar(null, diaAgendaId, brechas, usuario);
    }
}
