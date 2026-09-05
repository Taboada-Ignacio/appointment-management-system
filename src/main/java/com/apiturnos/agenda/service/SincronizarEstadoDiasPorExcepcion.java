package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
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
        var excepcionesActivas = excepcionRepository.findActivasAplicablesAFecha(profesionalId, fecha).stream()
                .filter(e -> e.aplicaEn(fecha))
                .toList();

        boolean habilitacionActiva = excepcionesActivas.stream()
                .anyMatch(e -> e.getTipo() == TipoExcepcion.HABILITACION_EXTRAORDINARIA);

        boolean modificacionActiva = excepcionesActivas.stream()
                .anyMatch(e -> e.getTipo() == TipoExcepcion.MODIFICACION_HORARIO);

        boolean cierreActivo = excepcionesActivas.stream()
                .anyMatch(e -> e.getTipo().esCierreDiaCompleto());

        var cambioActual = estados.obtenerCambioEstadoActual(AmbitoEstado.DIA_AGENDA, dia.getId());
        String estadoActual = cambioActual.map(c -> c.getEstado().getNombre()).orElse(null);
        String observacionActual = cambioActual.map(c -> c.getObservacion()).orElse("");

        // 1. Si hay habilitación extraordinaria o modificación horaria activa:
        // El día pasa a ACTIVO si estaba INACTIVO o no tenía estado registrado
        if (habilitacionActiva || modificacionActiva) {
            if ("INACTIVO".equals(estadoActual) || estadoActual == null) {
                String detalle = habilitacionActiva ? "habilitación extraordinaria" : "modificación horaria";
                estados.registrarCambio(AmbitoEstado.DIA_AGENDA, dia.getId(), "ACTIVO", usuario,
                        MARCA + ": " + detalle, null);
            }
            return;
        }

        // 2. Si hay cierre completo activo (y no hay habilitación ni modificación):
        // El día pasa a INACTIVO si estaba ACTIVO o EN_TRANSCURSO
        if (cierreActivo) {
            if ("ACTIVO".equals(estadoActual) || "EN_TRANSCURSO".equals(estadoActual)) {
                estados.registrarCambio(AmbitoEstado.DIA_AGENDA, dia.getId(), "INACTIVO", usuario,
                        MARCA + ": cierre completo", null);
            }
            return;
        }

        // 3. Ni habilitación ni modificación ni cierre activos:
        // Si el día fue activado por habilitación o modificación, debe volver a INACTIVO
        boolean fueActivadoPorHabilitacion = observacionActual != null && observacionActual.startsWith(MARCA + ": habilitación extraordinaria");
        boolean fueActivadoPorModificacion = observacionActual != null && observacionActual.startsWith(MARCA + ": modificación horaria");
        if ((fueActivadoPorHabilitacion || fueActivadoPorModificacion) && "ACTIVO".equals(estadoActual)) {
            String motivoFin = fueActivadoPorHabilitacion ? "fin habilitación extraordinaria" : "fin modificación horaria";
            estados.registrarCambio(AmbitoEstado.DIA_AGENDA, dia.getId(), "INACTIVO", usuario,
                    MARCA + ": " + motivoFin, null);
            return;
        }

        // Si el día fue inactivado por cierre completo, y el mes está ACTIVO, se restablece a ACTIVO
        boolean fueInactivadoPorCierre = observacionActual != null && observacionActual.startsWith(MARCA + ": cierre completo");
        String estadoMes = estados.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, dia.getMesAgenda().getId());
        if ("INACTIVO".equals(estadoActual) && fueInactivadoPorCierre && "ACTIVO".equals(estadoMes)) {
            estados.registrarCambio(AmbitoEstado.DIA_AGENDA, dia.getId(), "ACTIVO", usuario,
                    MARCA + ": fecha liberada", null);
        }
    }
}
