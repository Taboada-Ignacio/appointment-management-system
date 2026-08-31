package com.apiturnos.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigurarModoMesRequestDto {

    @NotNull(message = "La configuración de días del mes es obligatoria")
    @NotEmpty(message = "Debe enviar al menos un día para configurar")
    @Valid
    private List<DiaMesConfiguracionDto> dias = new ArrayList<>();

    public ConfigurarModoMesRequestDto() {
    }

    public ConfigurarModoMesRequestDto(List<DiaMesConfiguracionDto> dias) {
        this.dias = dias;
    }

    public List<DiaMesConfiguracionDto> getDias() {
        return dias;
    }

    public void setDias(List<DiaMesConfiguracionDto> dias) {
        this.dias = dias;
    }
}

