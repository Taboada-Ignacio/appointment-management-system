package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.AgendaAnual;

import java.time.Instant;

public class AgendaAnualResponseDto {

    private Long id;
    private Long profesionalId;
    private Integer anio;
    private Instant fechaCreacion;

    public AgendaAnualResponseDto() {
    }

    public AgendaAnualResponseDto(AgendaAnual agenda) {
        if (agenda != null) {
            this.id = agenda.getId();
            this.profesionalId = agenda.getProfesional() != null ? agenda.getProfesional().getId() : null;
            this.anio = agenda.getAnio();
            this.fechaCreacion = agenda.getFechaCreacion();
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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}

