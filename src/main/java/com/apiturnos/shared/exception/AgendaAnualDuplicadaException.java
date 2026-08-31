package com.apiturnos.shared.exception;

public class AgendaAnualDuplicadaException extends NegocioException {
    public AgendaAnualDuplicadaException(Long profesionalId, Integer anio) {
        super("Ya existe una agenda anual para el profesional " + profesionalId + " en el año " + anio);
    }
}
