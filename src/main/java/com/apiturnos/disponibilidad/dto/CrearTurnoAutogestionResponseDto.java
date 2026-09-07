package com.apiturnos.disponibilidad.dto;

import java.time.Instant;

public record CrearTurnoAutogestionResponseDto(
        Long turnoId, String estado, Instant inicioEstimado, Instant finEstimado) {
}
