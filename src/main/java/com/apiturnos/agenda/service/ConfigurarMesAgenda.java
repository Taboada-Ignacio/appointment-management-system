package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class ConfigurarMesAgenda {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public ConfigurarMesAgenda(MesAgendaRepository mesAgendaRepository,
                                DiaAgendaRepository diaAgendaRepository,
                                GestorCambioEstado gestorCambioEstado) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @Transactional
    public List<DiaAgenda> ejecutar(Long mesAgendaId) {
        return ejecutar(mesAgendaId, "sistema");
    }

    @Transactional
    public List<DiaAgenda> ejecutar(Long mesAgendaId, String usuario) {
        MesAgenda mesAgenda = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        int anio = mesAgenda.getAgendaAnual().getAnio();
        int nroMes = mesAgenda.getNroMes();
        YearMonth ym = YearMonth.of(anio, nroMes);
        int diasEnMes = ym.lengthOfMonth();

        for (int dia = 1; dia <= diasEnMes; dia++) {
            LocalDate fecha = LocalDate.of(anio, nroMes, dia);
            if (diaAgendaRepository.findByMesAgendaIdAndFecha(mesAgendaId, fecha).isEmpty()) {
                DiaAgenda diaAgenda = new DiaAgenda();
                diaAgenda.setMesAgenda(mesAgenda);
                diaAgenda.setFecha(fecha);
                diaAgendaRepository.save(diaAgenda);
            }
        }

        List<DiaAgenda> diasDelMes = diaAgendaRepository.findByMesAgendaId(mesAgendaId);
        String estadoMes = gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.MES_AGENDA, mesAgendaId);
        String estadoDia = "ACTIVO".equals(estadoMes) ? "ACTIVO" : "INACTIVO";
        for (DiaAgenda diaAgenda : diasDelMes) {
            if (gestorCambioEstado.obtenerCambioEstadoActual(
                    AmbitoEstado.DIA_AGENDA, diaAgenda.getId()).isEmpty()) {
                gestorCambioEstado.registrarCambioInicial(
                        AmbitoEstado.DIA_AGENDA, diaAgenda.getId(), estadoDia, usuario,
                        "Estado inicial al generar día de agenda");
            }
        }
        return diasDelMes;
    }
}
