package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.turno.model.Turno;

import java.util.List;

public record ResultadoAplicacionExcepcionAgenda(
        ExcepcionAgenda excepcion,
        List<Turno> turnosAfectados) {

    public ResultadoAplicacionExcepcionAgenda {
        turnosAfectados = List.copyOf(turnosAfectados);
    }
}
