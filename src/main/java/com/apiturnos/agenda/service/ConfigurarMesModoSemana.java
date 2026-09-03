package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.DiaAgendaNoValidoException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConfigurarMesModoSemana {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ConfigurarMesAgenda configurarMesAgenda;
    private final RegistradorAuditoria registradorAuditoria;
    private final GestorCambioEstado gestorCambioEstado;

    public ConfigurarMesModoSemana(MesAgendaRepository mesAgendaRepository,
                                  DiaAgendaRepository diaAgendaRepository,
                                  BrechaHorariaRepository brechaHorariaRepository,
                                  ConfigurarMesAgenda configurarMesAgenda,
                                  RegistradorAuditoria registradorAuditoria,
                                  GestorCambioEstado gestorCambioEstado) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.configurarMesAgenda = configurarMesAgenda;
        this.registradorAuditoria = registradorAuditoria;
        this.gestorCambioEstado = gestorCambioEstado;
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

        validarPlantillas(templates);

        // Asegurar que existan todos los DiaAgenda para el mes
        configurarMesAgenda.ejecutar(mesAgendaId);

        List<DiaAgenda> dias = diaAgendaRepository.findByMesAgendaId(mesAgendaId);
        boolean mesActivo = "ACTIVO".equals(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.MES_AGENDA, mesAgendaId));
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
            sincronizarEstadoDia(dia, mesActivo && templateBrechas != null && !templateBrechas.isEmpty(), usuario);
        }

        registradorAuditoria.registrar("AGENDA", "MesAgenda", mesAgendaId,
                OperacionAuditoria.UPDATE, usuario, profesionalId,
                "Mes configurado en modo SEMANA con " + (templates != null ? templates.size() : 0) + " plantillas");

        return mesAgenda;
    }

    public void validarPlantillas(List<DiaSemanaTemplate> templates) {
        if (templates == null) {
            return;
        }
        Set<DayOfWeek> diasUnicos = new HashSet<>();
        for (DiaSemanaTemplate template : templates) {
            if (template == null || template.getDiaSemana() == null || !diasUnicos.add(template.getDiaSemana())) {
                throw new DiaAgendaNoValidoException("No se permiten días de semana nulos o duplicados");
            }
            if (template.getBrechas() == null) {
                continue;
            }
            for (ConfigurarDiaAgenda.BrechaInput brecha : template.getBrechas()) {
                if (brecha == null || brecha.getHoraInicio() == null || brecha.getHoraFin() == null) {
                    throw new DiaAgendaNoValidoException("Las horas de inicio y fin son obligatorias");
                }
                if (!brecha.getHoraInicio().isBefore(brecha.getHoraFin())) {
                    throw new DiaAgendaNoValidoException(
                            "La hora de inicio (" + brecha.getHoraInicio() + ") debe ser anterior a la hora de fin (" + brecha.getHoraFin() + ")");
                }
            }
            List<ConfigurarDiaAgenda.BrechaInput> ordenadas = template.getBrechas().stream()
                    .sorted(java.util.Comparator.comparing(ConfigurarDiaAgenda.BrechaInput::getHoraInicio))
                    .toList();
            for (int i = 1; i < ordenadas.size(); i++) {
                if (ordenadas.get(i).getHoraInicio().isBefore(ordenadas.get(i - 1).getHoraFin())) {
                    throw new DiaAgendaNoValidoException(
                            "Las franjas horarias de " + template.getDiaSemana() + " no pueden superponerse");
                }
            }
        }
    }

    private void sincronizarEstadoDia(DiaAgenda dia, boolean debeEstarActivo, String usuario) {
        String destino = debeEstarActivo ? "ACTIVO" : "INACTIVO";
        String actual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, dia.getId());
        if (actual == null) {
            gestorCambioEstado.registrarCambioInicial(
                    AmbitoEstado.DIA_AGENDA, dia.getId(), destino, usuario, "Estado según plantilla semanal");
        } else if (!actual.equals(destino) && ("ACTIVO".equals(actual) || "INACTIVO".equals(actual))) {
            gestorCambioEstado.registrarCambio(
                    AmbitoEstado.DIA_AGENDA, dia.getId(), destino, usuario, "Estado según plantilla semanal", null);
        }
    }
}

