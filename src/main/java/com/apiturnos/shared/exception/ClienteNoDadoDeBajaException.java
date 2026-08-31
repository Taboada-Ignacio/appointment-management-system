package com.apiturnos.shared.exception;

public class ClienteNoDadoDeBajaException extends NegocioException {
    public ClienteNoDadoDeBajaException(Long clienteId, String estadoActual) {
        super("El cliente " + clienteId + " no está en estado DADO_DE_BAJA (estado actual: " + estadoActual + ")");
    }
}

