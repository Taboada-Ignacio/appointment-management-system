package com.apiturnos.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigurarDiaRequestDto {

    @NotNull(message = "La lista de brechas es obligatoria")
    @Valid
    private List<BrechaHorariaRequestDto> brechas = new ArrayList<>();

    public ConfigurarDiaRequestDto() {
    }

    public ConfigurarDiaRequestDto(List<BrechaHorariaRequestDto> brechas) {
        this.brechas = brechas;
    }

    public List<BrechaHorariaRequestDto> getBrechas() {
        return brechas;
    }

    public void setBrechas(List<BrechaHorariaRequestDto> brechas) {
        this.brechas = brechas;
    }
}

