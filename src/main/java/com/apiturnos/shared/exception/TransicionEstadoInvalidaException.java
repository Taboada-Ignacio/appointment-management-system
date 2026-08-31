package com.apiturnos.shared.exception;

public class TransicionEstadoInvalidaException extends NegocioException {
    public TransicionEstadoInvalidaException(String estadoActual, String estadoDestino, String ambito) {
        super("Transición inválida en " + ambito + ": " + estadoActual + " → " + estadoDestino);
    }
}
