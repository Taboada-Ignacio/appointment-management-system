package com.apiturnos.agenda.service;

import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.shared.exception.ExcepcionAgendaInvalidaException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidadorExcepcionAgenda {

    public void validar(SolicitudExcepcionAgenda solicitud) {
        if (solicitud == null) {
            throw new ExcepcionAgendaInvalidaException("La excepción de agenda es obligatoria");
        }
        if (solicitud.fechaInicio() == null || solicitud.fechaFin() == null) {
            throw new ExcepcionAgendaInvalidaException("El rango de fechas es obligatorio");
        }
        if (solicitud.fechaInicio().isAfter(solicitud.fechaFin())) {
            throw new ExcepcionAgendaInvalidaException("La fecha de inicio debe ser anterior o igual a la fecha de fin");
        }
        if (solicitud.tipo() == null) {
            throw new ExcepcionAgendaInvalidaException("El tipo de excepción es obligatorio");
        }
        if (solicitud.motivo() == null || solicitud.motivo().isBlank()) {
            throw new ExcepcionAgendaInvalidaException("El motivo de la excepción es obligatorio");
        }

        boolean tieneInicio = solicitud.horaInicio() != null;
        boolean tieneFin = solicitud.horaFin() != null;
        if (tieneInicio != tieneFin) {
            throw new ExcepcionAgendaInvalidaException("La hora de inicio y la hora de fin deben informarse juntas");
        }
        if (tieneInicio && tieneFin && !solicitud.horaInicio().isBefore(solicitud.horaFin())) {
            throw new ExcepcionAgendaInvalidaException("La hora de inicio debe ser anterior a la hora de fin");
        }

        List<IntervaloHorario> intervalos = solicitud.obtenerIntervalos();

        if (solicitud.tipo().requiereHorario()) {
            if (intervalos.isEmpty() && (!tieneInicio || !tieneFin)) {
                throw new ExcepcionAgendaInvalidaException(
                        "El tipo " + solicitud.tipo() + " requiere una franja horaria");
            }
        } else if (solicitud.tipo().esCierreDiaCompleto()) {
            if (tieneInicio || tieneFin || (solicitud.brechas() != null && !solicitud.brechas().isEmpty())) {
                throw new ExcepcionAgendaInvalidaException(
                        "El tipo " + solicitud.tipo() + " afecta el día completo y no admite una franja horaria");
            }
        }
    }
}
