package com.apiturnos.turno.service;

import com.apiturnos.shared.exception.NegocioException;

public class ReprogramacionTurnoInvalidaException extends NegocioException {
    public ReprogramacionTurnoInvalidaException(Long turnoId, String detalle) {
        super("No se puede reprogramar el Turno " + turnoId + ": " + detalle);
    }
}
