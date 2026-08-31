package com.apiturnos.turno.service;

import java.time.Instant;

public record SolicitudCrearTurnoManual(
        Long profesionalId,
        Long diaAgendaId,
        Long clienteId,
        Long tipoAtencionId,
        Instant inicioEstimado,
        Instant finEstimado,
        boolean confirmarAdvertencias,
        String observaciones,
        String usuario) {
}
