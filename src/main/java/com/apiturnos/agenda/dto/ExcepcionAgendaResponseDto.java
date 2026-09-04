package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExcepcionAgendaResponseDto(
        Long id,
        Long profesionalId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        TipoExcepcion tipo,
        List<BrechaExcepcionResponseDto> brechas,
        List<LocalDate> fechasExcluidas,
        String motivo,
        boolean activa,
        Instant fechaCreacion,
        Instant fechaModificacion) {

    public static ExcepcionAgendaResponseDto from(ExcepcionAgenda excepcion) {
        return new ExcepcionAgendaResponseDto(
                excepcion.getId(),
                excepcion.getProfesional().getId(),
                excepcion.getFechaInicio(),
                excepcion.getFechaFin(),
                excepcion.getTipo(),
                excepcion.obtenerIntervalos().stream().map(BrechaExcepcionResponseDto::from).toList(),
                excepcion.getFechasExcluidas().stream().sorted().toList(),
                excepcion.getMotivo(),
                excepcion.isActiva(),
                excepcion.getFechaCreacion(),
                excepcion.getFechaModificacion());
    }
}
