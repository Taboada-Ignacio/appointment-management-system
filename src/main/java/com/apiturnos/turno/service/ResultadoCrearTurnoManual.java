package com.apiturnos.turno.service;

import com.apiturnos.turno.model.AdvertenciaTurnoManual;

import java.util.List;

public record ResultadoCrearTurnoManual(
        boolean creado,
        boolean puedeCrear,
        boolean requiereConfirmacion,
        List<AdvertenciaTurnoManual> advertencias,
        DatosConfirmacionTurnoManual datosConfirmacion,
        Long turnoId) {

    public ResultadoCrearTurnoManual {
        advertencias = List.copyOf(advertencias);
    }

    public static ResultadoCrearTurnoManual requiereConfirmacion(
            List<AdvertenciaTurnoManual> advertencias,
            DatosConfirmacionTurnoManual datos) {
        return new ResultadoCrearTurnoManual(false, true, true, advertencias, datos, null);
    }

    public static ResultadoCrearTurnoManual creado(
            Long turnoId,
            List<AdvertenciaTurnoManual> advertencias,
            DatosConfirmacionTurnoManual datos) {
        return new ResultadoCrearTurnoManual(true, true, false, advertencias, datos, turnoId);
    }
}
