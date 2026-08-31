package com.apiturnos.agenda.dto;

import jakarta.validation.constraints.NotNull;

public class ActualizarRepetirConfiguracionRequestDto {

    @NotNull(message = "El campo repetirConfiguracion es obligatorio")
    private Boolean repetirConfiguracion;

    public ActualizarRepetirConfiguracionRequestDto() {
    }

    public ActualizarRepetirConfiguracionRequestDto(Boolean repetirConfiguracion) {
        this.repetirConfiguracion = repetirConfiguracion;
    }

    public Boolean getRepetirConfiguracion() {
        return repetirConfiguracion;
    }

    public void setRepetirConfiguracion(Boolean repetirConfiguracion) {
        this.repetirConfiguracion = repetirConfiguracion;
    }
}

