package com.apiturnos.agenda.dto;

import com.apiturnos.disponibilidad.model.IntervaloHorario;

import java.time.LocalTime;

public record BrechaExcepcionResponseDto(LocalTime horaInicio, LocalTime horaFin) {
    public static BrechaExcepcionResponseDto from(IntervaloHorario intervalo) {
        return new BrechaExcepcionResponseDto(intervalo.inicio(), intervalo.fin());
    }
}
