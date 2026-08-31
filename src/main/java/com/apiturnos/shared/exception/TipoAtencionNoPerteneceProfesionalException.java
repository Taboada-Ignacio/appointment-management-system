package com.apiturnos.shared.exception;

public class TipoAtencionNoPerteneceProfesionalException extends NegocioException {

    public TipoAtencionNoPerteneceProfesionalException(Long tipoAtencionId, Long profesionalId) {
        super("El tipo de atención " + tipoAtencionId + " no pertenece al profesional " + profesionalId);
    }
}

