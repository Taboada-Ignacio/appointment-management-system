package com.apiturnos.agenda.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class BrechaHorariaRequestDto {

    @NotNull(message = "La hora de inicio es obligatoria")
    @Schema(type = "string", example = "08:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @Schema(type = "string", example = "12:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    public BrechaHorariaRequestDto() {
    }

    public BrechaHorariaRequestDto(LocalTime horaInicio, LocalTime horaFin) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }
}

