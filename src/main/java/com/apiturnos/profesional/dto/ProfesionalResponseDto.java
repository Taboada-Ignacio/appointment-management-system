package com.apiturnos.profesional.dto;

import com.apiturnos.profesional.model.Profesional;
import java.time.Instant;

public class ProfesionalResponseDto {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String especialidad;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public ProfesionalResponseDto() {
    }

    public ProfesionalResponseDto(Profesional profesional) {
        if (profesional != null) {
            this.id = profesional.getId();
            this.nombre = profesional.getNombre();
            this.apellido = profesional.getApellido();
            this.email = profesional.getEmail();
            this.telefono = profesional.getTelefono();
            this.especialidad = profesional.getEspecialidad();
            this.creadoEn = profesional.getCreadoEn();
            this.actualizadoEn = profesional.getActualizadoEn();
        }
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(Instant creadoEn) {
        this.creadoEn = creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}

