package com.apiturnos.shared.exception;

public class CapacidadAgotadaException extends NegocioException {
    public CapacidadAgotadaException(int actual, int maximo) {
        super("Capacidad simultánea agotada: " + actual + "/" + maximo + " turnos en el intervalo");
    }
}
