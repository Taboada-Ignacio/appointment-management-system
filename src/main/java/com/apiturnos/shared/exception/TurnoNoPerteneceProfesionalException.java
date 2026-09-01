package com.apiturnos.shared.exception;

public class TurnoNoPerteneceProfesionalException extends NegocioException {

    public TurnoNoPerteneceProfesionalException(Long turnoId, Long profesionalId) {
        super("El turno " + turnoId + " no pertenece al profesional " + profesionalId);
    }
}

