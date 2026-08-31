package com.apiturnos.shared.exception;

public class ClienteNoPerteneceProfesionalException extends NegocioException {
    public ClienteNoPerteneceProfesionalException(Long clienteId, Long profesionalId) {
        super("El cliente " + clienteId + " no pertenece al profesional " + profesionalId);
    }
}
