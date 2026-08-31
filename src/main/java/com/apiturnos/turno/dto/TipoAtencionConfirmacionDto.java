package com.apiturnos.turno.dto;

public class TipoAtencionConfirmacionDto {

    private Long id;
    private String nombre;
    private Integer duracionMinutos;
    private Integer capacidadSimultanea;

    public TipoAtencionConfirmacionDto() {
    }

    public TipoAtencionConfirmacionDto(Long id, String nombre, Integer duracionMinutos, Integer capacidadSimultanea) {
        this.id = id;
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
        this.capacidadSimultanea = capacidadSimultanea;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

