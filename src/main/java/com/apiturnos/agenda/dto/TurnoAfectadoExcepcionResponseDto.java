package com.apiturnos.agenda.dto;

import java.time.Instant;
import java.time.LocalDate;

public record TurnoAfectadoExcepcionResponseDto(
        Long turnoId,
        LocalDate fecha,
        Instant inicioEstimado,
        Instant finEstimado,
        String estado,
        Long clienteId,
        String nombreCliente,
        String telefono,
        boolean notificacionWhatsAppHabilitada) {
}
