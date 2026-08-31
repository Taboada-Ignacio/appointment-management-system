package com.apiturnos.turno.service;

import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.model.MotivoRechazoTurnoManual;

public class TurnoManualNoPermitidoException extends NegocioException {

    private final MotivoRechazoTurnoManual motivo;

    public TurnoManualNoPermitidoException(MotivoRechazoTurnoManual motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public MotivoRechazoTurnoManual getMotivo() {
        return motivo;
    }
}
