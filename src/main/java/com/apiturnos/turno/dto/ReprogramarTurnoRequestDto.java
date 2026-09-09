package com.apiturnos.turno.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReprogramarTurnoRequestDto(
        String motivo,
        @NotNull Long nuevoDiaAgendaId,
        @NotNull Instant nuevoInicio,
        @NotNull Instant nuevoFin) {
}
