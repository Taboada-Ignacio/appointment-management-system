package com.apiturnos.agenda.dto;

import java.util.List;

public record ImpactoExcepcionAgendaResponseDto(
        String previewToken,
        int cantidadTurnosAfectados,
        int cantidadNotificacionesWhatsApp,
        int cantidadSinNotificacion,
        List<TurnoAfectadoExcepcionResponseDto> turnosAfectados) {

    public ImpactoExcepcionAgendaResponseDto {
        turnosAfectados = List.copyOf(turnosAfectados);
    }

    public ImpactoExcepcionAgendaResponseDto(
            int cantidadTurnosAfectados,
            int cantidadNotificacionesWhatsApp,
            int cantidadSinNotificacion,
            List<TurnoAfectadoExcepcionResponseDto> turnosAfectados) {
        this(null, cantidadTurnosAfectados, cantidadNotificacionesWhatsApp,
                cantidadSinNotificacion, turnosAfectados);
    }
}
