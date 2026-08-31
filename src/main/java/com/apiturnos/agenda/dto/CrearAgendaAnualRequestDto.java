package com.apiturnos.agenda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CrearAgendaAnualRequestDto {

    @NotNull(message = "El año es obligatorio")
    @Min(value = 2020, message = "El año debe ser mayor o igual a 2020")
    @Max(value = 2100, message = "El año debe ser menor o igual a 2100")
    private Integer anio;

    public CrearAgendaAnualRequestDto() {
    }

    public CrearAgendaAnualRequestDto(Integer anio) {
        this.anio = anio;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }
}

