package com.apiturnos.turno.dto;

import jakarta.validation.constraints.NotBlank;

public class DarDeBajaTurnoRequestDto {

    @NotBlank(message = "El motivo de baja es obligatorio")
    private String motivo;

    public DarDeBajaTurnoRequestDto() {
    }

    public DarDeBajaTurnoRequestDto(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

