package com.apiturnos.profesional.dto;

import jakarta.validation.constraints.Min;

public class ConfiguracionRequestDto {

    @Min(value = 1, message = "La cantidad máxima de turnos a la vez debe ser mayor a 0")
    private Integer cantidadMaxTurnosALaVez;

    @Min(value = 1, message = "La duración aproximada por turno debe ser mayor a 0")
    private Integer duracionAproximadaPorTurno;

    private Boolean agendaSoloManejadaPorProfesional;

    @Min(value = 0, message = "El umbral de cancelación en horas no puede ser negativo")
    private Integer umbralCancelacionHoras;

    public ConfiguracionRequestDto() {
    }

    public ConfiguracionRequestDto(Integer cantidadMaxTurnosALaVez, Integer duracionAproximadaPorTurno,
                                  Boolean agendaSoloManejadaPorProfesional, Integer umbralCancelacionHoras) {
        this.cantidadMaxTurnosALaVez = cantidadMaxTurnosALaVez;
        this.duracionAproximadaPorTurno = duracionAproximadaPorTurno;
        this.agendaSoloManejadaPorProfesional = agendaSoloManejadaPorProfesional;
        this.umbralCancelacionHoras = umbralCancelacionHoras;
    }

    public Integer getCantidadMaxTurnosALaVez() {
        return cantidadMaxTurnosALaVez;
    }

    public void setCantidadMaxTurnosALaVez(Integer cantidadMaxTurnosALaVez) {
        this.cantidadMaxTurnosALaVez = cantidadMaxTurnosALaVez;
    }

    public Integer getDuracionAproximadaPorTurno() {
        return duracionAproximadaPorTurno;
    }

    public void setDuracionAproximadaPorTurno(Integer duracionAproximadaPorTurno) {
        this.duracionAproximadaPorTurno = duracionAproximadaPorTurno;
    }

    public Boolean getAgendaSoloManejadaPorProfesional() {
        return agendaSoloManejadaPorProfesional;
    }

    public void setAgendaSoloManejadaPorProfesional(Boolean agendaSoloManejadaPorProfesional) {
        this.agendaSoloManejadaPorProfesional = agendaSoloManejadaPorProfesional;
    }

    public Integer getUmbralCancelacionHoras() {
        return umbralCancelacionHoras;
    }

    public void setUmbralCancelacionHoras(Integer umbralCancelacionHoras) {
        this.umbralCancelacionHoras = umbralCancelacionHoras;
    }
}

