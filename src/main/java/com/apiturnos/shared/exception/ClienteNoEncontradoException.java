package com.apiturnos.shared.exception;

public class ClienteNoEncontradoException extends EntidadNoEncontradaException {
    public ClienteNoEncontradoException(Long id) {
        super("Cliente con id " + id + " no encontrado");
    }

    public ClienteNoEncontradoException(Long profesionalId, Long clienteId) {
        super("Cliente con id " + clienteId + " no encontrado para el profesional " + profesionalId);
    }

    public ClienteNoEncontradoException(String message) {
        super(message);
    }
}

