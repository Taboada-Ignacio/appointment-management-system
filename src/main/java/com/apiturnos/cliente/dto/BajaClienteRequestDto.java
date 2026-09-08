package com.apiturnos.cliente.dto;

import jakarta.validation.constraints.Size;

public class BajaClienteRequestDto {
    @Size(max = 500)
    private String motivo;

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
