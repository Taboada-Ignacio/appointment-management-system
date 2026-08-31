package com.apiturnos.shared.exception;

public class EntidadNoEncontradaException extends NegocioException {
    public EntidadNoEncontradaException(String entidad, Long id) {
        super(entidad + " con id " + id + " no encontrada");
    }
    public EntidadNoEncontradaException(String message) {
        super(message);
    }
}
