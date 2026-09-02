package com.apiturnos.shared.exception;

public class ProfesionalDuplicadoException extends NegocioException {
    public ProfesionalDuplicadoException(String email) {
        super("Ya existe un profesional registrado con el email: " + email);
    }
}

