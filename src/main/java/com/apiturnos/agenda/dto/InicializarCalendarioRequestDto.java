package com.apiturnos.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InicializarCalendarioRequestDto {

    @NotNull(message = "La configuración de días es obligatoria")
    @NotEmpty(message = "Debe seleccionar al menos un día laborable")
    @Valid
    private List<DiaSemanaConfiguracionDto> diasSemana = new ArrayList<>();

    private Boolean repetirAlMesSiguiente = true;

    public List<DiaSemanaConfiguracionDto> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(List<DiaSemanaConfiguracionDto> diasSemana) {
        this.diasSemana = diasSemana;
    }

    public Boolean getRepetirAlMesSiguiente() {
        return repetirAlMesSiguiente;
    }

    public void setRepetirAlMesSiguiente(Boolean repetirAlMesSiguiente) {
        this.repetirAlMesSiguiente = repetirAlMesSiguiente;
    }
}
