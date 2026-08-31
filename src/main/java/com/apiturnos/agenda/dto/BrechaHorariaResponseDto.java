package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

public class BrechaHorariaResponseDto {

    private Long id;

    @Schema(type = "string", example = "08:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @Schema(type = "string", example = "12:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    public BrechaHorariaResponseDto() {
    }

    public BrechaHorariaResponseDto(BrechaHoraria brecha) {
        if (brecha != null) {
            this.id = brecha.getId();
            this.horaInicio = brecha.getHoraInicioAtencion();
            this.horaFin = brecha.getHoraFinAtencion();
        }
    }

    public BrechaHorariaResponseDto(Long id, LocalTime horaInicio, LocalTime horaFin) {
        this.id = id;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

