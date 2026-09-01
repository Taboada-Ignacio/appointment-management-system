package com.apiturnos.turno.dto;

public class CancelarTurnoRequestDto {

    private String motivo;

    public CancelarTurnoRequestDto() {
    }

    public CancelarTurnoRequestDto(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

