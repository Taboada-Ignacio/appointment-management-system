package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.EstadoResolucionAfectacion;
import com.apiturnos.agenda.model.TipoExcepcion;

import java.time.Instant;
import java.time.LocalDate;

public record AfectacionTurnoResponseDto(
        Long afectacionId,
        Long excepcionId,
        TipoExcepcion tipoExcepcion,
        String motivoExcepcion,
        boolean excepcionActiva,
        Long turnoId,
        EstadoResolucionAfectacion resolucion,
        String estadoTurnoAnterior,
        String estadoTurno,
        Long clienteId,
        String nombreCliente,
        String telefono,
        LocalDate fechaOriginal,
        Instant inicioOriginal,
        Instant finOriginal,
        LocalDate fechaActual,
        Instant inicioActual,
        Instant finActual,
        String observacion,
        Instant creadoEn,
        Instant resueltoEn) {
}
