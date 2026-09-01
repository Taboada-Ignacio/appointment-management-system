package com.apiturnos.shared.exception;

public class TransicionEstadoInvalidaException extends NegocioException {
    private final String estadoActual;
    private final String estadoDestino;
    private final String ambito;
    private final Long entidadId;

    public TransicionEstadoInvalidaException(String estadoActual, String estadoDestino, String ambito) {
        this(estadoActual, estadoDestino, ambito, null);
    }

    public TransicionEstadoInvalidaException(String estadoActual, String estadoDestino,
                                             String ambito, Long entidadId) {
        super("Transicion invalida en " + ambito
                + (entidadId != null ? " " + entidadId : "")
                + ": " + estadoActual + " -> " + estadoDestino);
        this.estadoActual = estadoActual;
        this.estadoDestino = estadoDestino;
        this.ambito = ambito;
        this.entidadId = entidadId;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public String getEstadoDestino() {
        return estadoDestino;
    }

    public String getAmbito() {
        return ambito;
    }

    public Long getEntidadId() {
        return entidadId;
    }
}
