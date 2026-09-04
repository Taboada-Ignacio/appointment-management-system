package com.apiturnos.agenda.dto;

import java.time.Instant;

public record ResolverAfectacionRequestDto(
        String observacion,
        Long nuevoDiaAgendaId,
        Instant nuevoInicio,
        Instant nuevoFin) {
}
