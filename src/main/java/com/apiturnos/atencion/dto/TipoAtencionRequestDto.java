package com.apiturnos.atencion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TipoAtencionRequestDto {

    @NotBlank(message = "El nombre del tipo de atención es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La duración en minutos es obligatoria")
    @Min(value = 1, message = "La duración en minutos debe ser mayor a 0")
    private Integer duracionMinutos;

    @NotNull(message = "La capacidad simultánea es obligatoria")
    @Min(value = 1, message = "La capacidad simultánea debe ser mayor o igual a 1")
    private Integer capacidadSimultanea = 1;

    public TipoAtencionRequestDto() {
    }

    public TipoAtencionRequestDto(String nombre, String descripcion, Integer duracionMinutos, Integer capacidadSimultanea) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionMinutos = duracionMinutos;
        this.capacidadSimultanea = capacidadSimultanea;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Integer getCapacidadSimultanea() {
        return capacidadSimultanea;
    }

    public void setCapacidadSimultanea(Integer capacidadSimultanea) {
        this.capacidadSimultanea = capacidadSimultanea;
    }
}

