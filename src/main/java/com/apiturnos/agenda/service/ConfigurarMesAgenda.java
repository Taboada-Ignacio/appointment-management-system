package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConfigurarMesAgenda {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;

    public ConfigurarMesAgenda(MesAgendaRepository mesAgendaRepository,
                                DiaAgendaRepository diaAgendaRepository) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
    }

    @Transactional
    public List<DiaAgenda> ejecutar(Long mesAgendaId) {
        MesAgenda mesAgenda = mesAgendaRepository.findById(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        int anio = mesAgenda.getAgendaAnual().getAnio();
        int nroMes = mesAgenda.getNroMes();
        YearMonth ym = YearMonth.of(anio, nroMes);
        int diasEnMes = ym.lengthOfMonth();

        List<DiaAgenda> diasCreados = new ArrayList<>();
        for (int dia = 1; dia <= diasEnMes; dia++) {
            LocalDate fecha = LocalDate.of(anio, nroMes, dia);
            if (diaAgendaRepository.findByMesAgendaIdAndFecha(mesAgendaId, fecha).isEmpty()) {
                DiaAgenda diaAgenda = new DiaAgenda();
                diaAgenda.setMesAgenda(mesAgenda);
                diaAgenda.setFecha(fecha);
                diasCreados.add(diaAgendaRepository.save(diaAgenda));
            }
        }
        return diasCreados;
    }
}
