package com.apiturnos.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DiaMesConfiguracionDto {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La lista de brechas es obligatoria")
    @Valid
    private List<BrechaHorariaRequestDto> brechas = new ArrayList<>();

    public DiaMesConfiguracionDto() {
    }

    public DiaMesConfiguracionDto(LocalDate fecha, List<BrechaHorariaRequestDto> brechas) {
        this.fecha = fecha;
        this.brechas = brechas;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public List<BrechaHorariaRequestDto> getBrechas() {
        return brechas;
    }

    public void setBrechas(List<BrechaHorariaRequestDto> brechas) {
        this.brechas = brechas;
    }
}
