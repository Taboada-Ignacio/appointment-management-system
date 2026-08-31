package com.apiturnos.turno.service;

import com.apiturnos.turno.model.AdvertenciaTurnoManual;

import java.time.LocalTime;
import java.util.List;

public record HorarioSugeridoTurnoManual(
        LocalTime horaInicio,
        LocalTime horaFin,
        int duracionMinutos,
        int turnosConcurrentes,
        int capacidadSimultanea,
        List<AdvertenciaTurnoManual> advertencias) {

    public HorarioSugeridoTurnoManual {
        advertencias = List.copyOf(advertencias);
    }
}
