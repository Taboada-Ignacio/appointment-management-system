package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class SincronizarEstadoDiasPorExcepcion {
    private static final String MARCA = "Estado administrado por excepción de agenda";

    private final DiaAgendaRepository diaRepository;
    private final ExcepcionAgendaRepository excepcionRepository;
    private final GestorCambioEstado estados;

    public SincronizarEstadoDiasPorExcepcion(
            DiaAgendaRepository diaRepository,
            ExcepcionAgendaRepository excepcionRepository,
            GestorCambioEstado estados) {
        this.diaRepository = diaRepository;
        this.excepcionRepository = excepcionRepository;
        this.estados = estados;
    }

    public void reconciliar(Long profesionalId, Collection<LocalDate> fechas, String usuario) {
        for (LocalDate fecha : new LinkedHashSet<>(fechas)) {
            diaRepository.findByProfesionalIdAndFecha(profesionalId, fecha)
                    .ifPresent(dia -> reconciliarDia(profesionalId, dia, fecha, usuario));
        }
    }

    public static Set<LocalDate> fechasEfectivas(ExcepcionAgenda excepcion) {
        Set<LocalDate> fechas = new LinkedHashSet<>();
        for (LocalDate fecha = excepcion.getFechaInicio(); !fecha.isAfter(excepcion.getFechaFin()); fecha = fecha.plusDays(1)) {
            if (excepcion.aplicaEn(fecha)) fechas.add(fecha);
        }
        return fechas;
    }

    private void reconciliarDia(Long profesionalId, DiaAgenda dia, LocalDate fecha, String usuario) {
        boolean cierreActivo = excepcionRepository.findActivasAplicablesAFecha(profesionalId, fecha).stream()
                .anyMatch(e -> e.aplicaEn(fecha) && e.getTipo().esCierreDiaCompleto());
        var cambioActual = estados.obtenerCambioEstadoActual(AmbitoEstado.DIA_AGENDA, dia.getId());
        String estadoActual = cambioActual.map(c -> c.getEstado().getNombre()).orElse(null);

        if (cierreActivo && "ACTIVO".equals(estadoActual)) {
            estados.registrarCambio(AmbitoEstado.DIA_AGENDA, dia.getId(), "INACTIVO", usuario,
                    MARCA + ": cierre completo", null);
            return;
        }

        boolean fueInactivadoPorExcepcion = cambioActual
                .map(c -> c.getObservacion() != null && c.getObservacion().startsWith(MARCA))
                .orElse(false);
        String estadoMes = estados.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, dia.getMesAgenda().getId());
        if (!cierreActivo && "INACTIVO".equals(estadoActual) && fueInactivadoPorExcepcion
                && "ACTIVO".equals(estadoMes)) {
            estados.registrarCambio(AmbitoEstado.DIA_AGENDA, dia.getId(), "ACTIVO", usuario,
                    MARCA + ": fecha liberada", null);
        }
    }
}
