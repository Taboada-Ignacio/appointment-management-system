package com.apiturnos.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class DiaSemanaConfiguracionDto {

    @NotNull(message = "El día de la semana es obligatorio")
    private DayOfWeek diaSemana;

    @NotNull(message = "La lista de brechas es obligatoria")
    @Valid
    private List<BrechaHorariaRequestDto> brechas = new ArrayList<>();

    public DiaSemanaConfiguracionDto() {
    }

    public DiaSemanaConfiguracionDto(DayOfWeek diaSemana, List<BrechaHorariaRequestDto> brechas) {
        this.diaSemana = diaSemana;
        this.brechas = brechas;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DayOfWeek diaSemana) {
        this.diaSemana = diaSemana;
    }

    public List<BrechaHorariaRequestDto> getBrechas() {
        return brechas;
    }

    public void setBrechas(List<BrechaHorariaRequestDto> brechas) {
        this.brechas = brechas;
    }
}

