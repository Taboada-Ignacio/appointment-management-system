package com.apiturnos.turno.service;

import com.apiturnos.shared.exception.NegocioException;

import java.time.Instant;

public class TurnoYaIniciadoException extends NegocioException {

    public TurnoYaIniciadoException(Long turnoId, Instant inicioEstimado, Instant ahora) {
        super("El Turno " + turnoId + " ya inició o alcanzó su inicio estimado (inicio="
                + inicioEstimado + ", ahora=" + ahora + ") y no admite cancelación ordinaria");
    }
}
