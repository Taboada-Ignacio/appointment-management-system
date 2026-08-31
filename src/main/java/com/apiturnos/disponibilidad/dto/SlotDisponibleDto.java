package com.apiturnos.disponibilidad.dto;

import java.time.LocalTime;

public record SlotDisponibleDto(
        LocalTime horaInicio,
        LocalTime horaFin,
        int duracionMinutos,
        int turnosConcurrentes,
        int capacidadMaxima) {
}

