package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepetirConfiguracionMes {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;

    public RepetirConfiguracionMes(MesAgendaRepository mesAgendaRepository,
                                    DiaAgendaRepository diaAgendaRepository,
                                    BrechaHorariaRepository brechaHorariaRepository) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
    }

    @Transactional
    public MesAgenda ejecutar(Long mesOrigenId) {
        MesAgenda mesOrigen = mesAgendaRepository.findById(mesOrigenId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesOrigenId));

        if (!Boolean.TRUE.equals(mesOrigen.getRepetirConfiguracion())) {
            throw new NegocioException("El mes no tiene activada la repetición de configuración");
        }

        Long agendaAnualId = mesOrigen.getAgendaAnual().getId();
        int nroMesSiguiente = mesOrigen.getNroMes() + 1;

        // Handle year boundary (December -> need next year's agenda)
        if (nroMesSiguiente > 12) {
            throw new NegocioException("No se puede repetir configuración de diciembre al siguiente año desde esta agenda");
        }

        MesAgenda mesDestino = mesAgendaRepository.findByAgendaAnualIdAndNroMes(agendaAnualId, nroMesSiguiente)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "MesAgenda para mes " + nroMesSiguiente + " no encontrado"));

        // Get source days and group by day-of-week
        List<DiaAgenda> diasOrigen = diaAgendaRepository.findByMesAgendaId(mesOrigenId);
        Map<DayOfWeek, List<BrechaHoraria>> templatePorDia = new HashMap<>();
        for (DiaAgenda dia : diasOrigen) {
            DayOfWeek dow = dia.getFecha().getDayOfWeek();
            if (!templatePorDia.containsKey(dow)) {
                List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
                if (!brechas.isEmpty()) {
                    templatePorDia.put(dow, brechas);
                }
            }
        }

        // Create days in target month
        int anio = mesOrigen.getAgendaAnual().getAnio();
        YearMonth ymDestino = YearMonth.of(anio, nroMesSiguiente);
        for (int d = 1; d <= ymDestino.lengthOfMonth(); d++) {
            LocalDate fecha = LocalDate.of(anio, nroMesSiguiente, d);
            DayOfWeek dow = fecha.getDayOfWeek();
            List<BrechaHoraria> templateBrechas = templatePorDia.get(dow);
            if (templateBrechas != null) {
                DiaAgenda nuevoDia = diaAgendaRepository.findByMesAgendaIdAndFecha(mesDestino.getId(), fecha)
                        .orElseGet(() -> {
                            DiaAgenda nd = new DiaAgenda();
                            nd.setMesAgenda(mesDestino);
                            nd.setFecha(fecha);
                            return diaAgendaRepository.save(nd);
                        });

                // Only copy brechas if the day doesn't already have brechas
                if (brechaHorariaRepository.findByDiaAgendaId(nuevoDia.getId()).isEmpty()) {
                    for (BrechaHoraria template : templateBrechas) {
                        BrechaHoraria nueva = new BrechaHoraria();
                        nueva.setDiaAgenda(nuevoDia);
                        nueva.setHoraInicioAtencion(template.getHoraInicioAtencion());
                        nueva.setHoraFinAtencion(template.getHoraFinAtencion());
                        brechaHorariaRepository.save(nueva);
                    }
                }
            }
        }

        return mesDestino;
    }
}
