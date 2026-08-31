package com.apiturnos.shared.exception;

public class ClienteNoPendienteDeVerificacionException extends NegocioException {
    public ClienteNoPendienteDeVerificacionException(Long clienteId, String estadoActual) {
        super("El cliente " + clienteId + " no está en estado PENDIENTE_DE_VERIFICACION (estado actual: " + estadoActual + ")");
    }
}

