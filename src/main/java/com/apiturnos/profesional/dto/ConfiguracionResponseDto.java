package com.apiturnos.profesional.dto;

import com.apiturnos.profesional.model.Configuracion;
import java.time.Instant;

public class ConfiguracionResponseDto {

    private Long id;
    private Long profesionalId;
    private Integer cantidadMaxTurnosALaVez;
    private Integer duracionAproximadaPorTurno;
    private Boolean agendaSoloManejadaPorProfesional;
    private Integer umbralCancelacionHoras;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public ConfiguracionResponseDto() {
    }

    public ConfiguracionResponseDto(Configuracion configuracion) {
        if (configuracion != null) {
            this.id = configuracion.getId();
            this.profesionalId = configuracion.getProfesional() != null ? configuracion.getProfesional().getId() : null;
            this.cantidadMaxTurnosALaVez = configuracion.getCantidadMaxTurnosALaVez();
            this.duracionAproximadaPorTurno = configuracion.getDuracionAproximadaPorTurno();
            this.agendaSoloManejadaPorProfesional = configuracion.getAgendaSoloManejadaPorProfesional();
            this.umbralCancelacionHoras = configuracion.getUmbralCancelacionHoras();
            this.creadoEn = configuracion.getCreadoEn();
            this.actualizadoEn = configuracion.getActualizadoEn();
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

    public Integer getCantidadMaxTurnosALaVez() {
        return cantidadMaxTurnosALaVez;
    }

    public void setCantidadMaxTurnosALaVez(Integer cantidadMaxTurnosALaVez) {
        this.cantidadMaxTurnosALaVez = cantidadMaxTurnosALaVez;
    }

    public Integer getDuracionAproximadaPorTurno() {
        return duracionAproximadaPorTurno;
    }

    public void setDuracionAproximadaPorTurno(Integer duracionAproximadaPorTurno) {
        this.duracionAproximadaPorTurno = duracionAproximadaPorTurno;
    }

    public Boolean getAgendaSoloManejadaPorProfesional() {
        return agendaSoloManejadaPorProfesional;
    }

    public void setAgendaSoloManejadaPorProfesional(Boolean agendaSoloManejadaPorProfesional) {
        this.agendaSoloManejadaPorProfesional = agendaSoloManejadaPorProfesional;
    }

    public Integer getUmbralCancelacionHoras() {
        return umbralCancelacionHoras;
    }

    public void setUmbralCancelacionHoras(Integer umbralCancelacionHoras) {
        this.umbralCancelacionHoras = umbralCancelacionHoras;
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

