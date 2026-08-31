package com.apiturnos.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigurarModoSemanaRequestDto {

    @NotNull(message = "La configuración de días de la semana es obligatoria")
    @NotEmpty(message = "Debe enviar al menos un día de la semana para configurar")
    @Valid
    private List<DiaSemanaConfiguracionDto> diasSemana = new ArrayList<>();

    public ConfigurarModoSemanaRequestDto() {
    }

    public ConfigurarModoSemanaRequestDto(List<DiaSemanaConfiguracionDto> diasSemana) {
        this.diasSemana = diasSemana;
    }

    public List<DiaSemanaConfiguracionDto> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(List<DiaSemanaConfiguracionDto> diasSemana) {
        this.diasSemana = diasSemana;
    }
}

