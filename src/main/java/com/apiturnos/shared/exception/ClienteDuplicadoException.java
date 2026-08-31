package com.apiturnos.shared.exception;

public class ClienteDuplicadoException extends NegocioException {
    public ClienteDuplicadoException(Long profesionalId, String numeroDocumento) {
        super("Ya existe un cliente con documento " + numeroDocumento + " para el profesional " + profesionalId);
    }
}
