package com.apiturnos.agenda.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record DecisionTurnoAfectadoRequestDto(
        @NotNull Long turnoId,
        @NotNull TipoDecisionTurnoAfectado decision,
        Long nuevoDiaAgendaId,
        Instant nuevoInicio,
        Instant nuevoFin,
        String observacion) {
}
