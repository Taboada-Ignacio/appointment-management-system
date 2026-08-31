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

import java.time.LocalDate;
import java.util.List;

@Service
public class ConfigurarMesModoMes {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ConfigurarMesAgenda configurarMesAgenda;
    private final RegistradorAuditoria registradorAuditoria;

    public ConfigurarMesModoMes(MesAgendaRepository mesAgendaRepository,
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

    public static class DiaMesTemplate {
        private final LocalDate fecha;
        private final List<ConfigurarDiaAgenda.BrechaInput> brechas;

        public DiaMesTemplate(LocalDate fecha, List<ConfigurarDiaAgenda.BrechaInput> brechas) {
            this.fecha = fecha;
            this.brechas = brechas;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public List<ConfigurarDiaAgenda.BrechaInput> getBrechas() {
            return brechas;
        }
    }

    @Transactional
    public MesAgenda ejecutar(Long profesionalId, Long mesAgendaId, List<DiaMesTemplate> diasConfig, String usuario) {
        MesAgenda mesAgenda = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (!mesAgenda.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        int anio = mesAgenda.getAgendaAnual().getAnio();
        int nroMes = mesAgenda.getNroMes();

        // Asegurar que existan los días del mes
        configurarMesAgenda.ejecutar(mesAgendaId);

        if (diasConfig != null) {
            for (DiaMesTemplate d : diasConfig) {
                if (d.getFecha().getYear() != anio || d.getFecha().getMonthValue() != nroMes) {
                    throw new DiaAgendaNoValidoException(
                            "La fecha " + d.getFecha() + " no corresponde al mes " + nroMes + " del año " + anio);
                }

                if (d.getBrechas() != null) {
                    for (ConfigurarDiaAgenda.BrechaInput b : d.getBrechas()) {
                        if (!b.getHoraInicio().isBefore(b.getHoraFin())) {
                            throw new DiaAgendaNoValidoException(
                                    "La hora de inicio (" + b.getHoraInicio() + ") debe ser anterior a la hora de fin (" + b.getHoraFin() + ")");
                        }
                    }
                }

                DiaAgenda dia = diaAgendaRepository.findByMesAgendaIdAndFecha(mesAgendaId, d.getFecha())
                        .orElseGet(() -> {
                            DiaAgenda nd = new DiaAgenda();
                            nd.setMesAgenda(mesAgenda);
                            nd.setFecha(d.getFecha());
                            return diaAgendaRepository.save(nd);
                        });

                List<BrechaHoraria> existentes = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
                brechaHorariaRepository.deleteAll(existentes);

                if (d.getBrechas() != null) {
                    for (ConfigurarDiaAgenda.BrechaInput b : d.getBrechas()) {
                        BrechaHoraria nueva = new BrechaHoraria();
                        nueva.setDiaAgenda(dia);
                        nueva.setHoraInicioAtencion(b.getHoraInicio());
                        nueva.setHoraFinAtencion(b.getHoraFin());
                        brechaHorariaRepository.save(nueva);
                    }
                }
            }
        }

        registradorAuditoria.registrar("AGENDA", "MesAgenda", mesAgendaId,
                OperacionAuditoria.UPDATE, usuario, profesionalId,
                "Mes configurado en modo MES para " + (diasConfig != null ? diasConfig.size() : 0) + " fechas");

        return mesAgenda;
    }
}

