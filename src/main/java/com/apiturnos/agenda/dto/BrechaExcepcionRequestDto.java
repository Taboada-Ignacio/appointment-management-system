package com.apiturnos.agenda.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record BrechaExcepcionRequestDto(
        @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin) {
}
