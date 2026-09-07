package com.apiturnos.disponibilidad.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CrearTurnoAutogestionRequestDto(
        @NotNull Long diaAgendaId,
        @NotNull Long clienteId,
        @NotNull Long tipoAtencionId,
        @NotNull Instant inicioEstimado,
        String observaciones) {
}
