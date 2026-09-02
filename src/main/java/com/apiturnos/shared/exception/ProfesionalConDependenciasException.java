package com.apiturnos.shared.exception;

public class ProfesionalConDependenciasException extends NegocioException {

    public ProfesionalConDependenciasException(Long profesionalId, Throwable cause) {
        super("No se puede eliminar el profesional con id " + profesionalId
                + " porque tiene información asociada", cause);
    }
}
