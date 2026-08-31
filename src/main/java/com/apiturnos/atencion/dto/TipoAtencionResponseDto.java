package com.apiturnos.atencion.dto;

import com.apiturnos.atencion.model.TipoAtencion;

public class TipoAtencionResponseDto {

    private Long id;
    private Long profesionalId;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private Integer capacidadSimultanea;
    private Boolean activo;

    public TipoAtencionResponseDto() {
    }

    public TipoAtencionResponseDto(TipoAtencion tipo) {
        if (tipo != null) {
            this.id = tipo.getId();
            this.profesionalId = tipo.getProfesional() != null ? tipo.getProfesional().getId() : null;
            this.nombre = tipo.getNombre();
            this.descripcion = tipo.getDescripcion();
            this.duracionMinutos = tipo.getDuracionMinutos();
            this.capacidadSimultanea = tipo.getCapacidadSimultanea();
            this.activo = tipo.getActivo();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Long profesionalId) {
        this.profesionalId = profesionalId;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}

