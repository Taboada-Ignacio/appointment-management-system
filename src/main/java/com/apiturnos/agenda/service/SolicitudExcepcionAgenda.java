package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SolicitudExcepcionAgenda(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        TipoExcepcion tipo,
        LocalTime horaInicio,
        LocalTime horaFin,
        List<IntervaloHorario> brechas,
        String motivo) {

    public SolicitudExcepcionAgenda(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoExcepcion tipo,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo) {
        this(fechaInicio,
                fechaFin,
                tipo,
                horaInicio,
                horaFin,
                (horaInicio != null && horaFin != null && horaInicio.isBefore(horaFin))
                        ? List.of(new IntervaloHorario(horaInicio, horaFin))
                        : List.of(),
                motivo);
    }

    public SolicitudExcepcionAgenda(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoExcepcion tipo,
            List<IntervaloHorario> brechas,
            String motivo) {
        this(fechaInicio,
                fechaFin,
                tipo,
                (brechas != null && !brechas.isEmpty()) ? brechas.getFirst().inicio() : null,
                (brechas != null && !brechas.isEmpty()) ? brechas.getLast().fin() : null,
                (brechas != null) ? List.copyOf(brechas) : List.of(),
                motivo);
    }

    public List<IntervaloHorario> obtenerIntervalos() {
        if (brechas != null && !brechas.isEmpty()) {
            return brechas;
        }
        if (horaInicio != null && horaFin != null && horaInicio.isBefore(horaFin)) {
            return List.of(new IntervaloHorario(horaInicio, horaFin));
        }
        return List.of();
    }
}
