package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.MesAgenda;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MesAgendaDetalleResponseDto {

    private Long id;
    private Long agendaAnualId;
    private Integer anio;
    private Integer nroMes;
    private String nombreMes;
    private Boolean repetirConfiguracion;
    private String estadoActual;
    private List<DiaAgendaResumenResponseDto> dias = new ArrayList<>();

    public MesAgendaDetalleResponseDto() {
    }

    public MesAgendaDetalleResponseDto(MesAgenda mes, String estadoActual, List<DiaAgendaResumenResponseDto> dias) {
        if (mes != null) {
            this.id = mes.getId();
            this.agendaAnualId = mes.getAgendaAnual() != null ? mes.getAgendaAnual().getId() : null;
            this.anio = mes.getAgendaAnual() != null ? mes.getAgendaAnual().getAnio() : null;
            this.nroMes = mes.getNroMes();
            if (mes.getNroMes() != null && mes.getNroMes() >= 1 && mes.getNroMes() <= 12) {
                this.nombreMes = Month.of(mes.getNroMes()).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            }
            this.repetirConfiguracion = mes.getRepetirConfiguracion();
        }
        this.estadoActual = estadoActual;
        this.dias = dias != null ? dias : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgendaAnualId() {
        return agendaAnualId;
    }

    public void setAgendaAnualId(Long agendaAnualId) {
        this.agendaAnualId = agendaAnualId;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getNroMes() {
        return nroMes;
    }

    public void setNroMes(Integer nroMes) {
        this.nroMes = nroMes;
    }

    public String getNombreMes() {
        return nombreMes;
    }

    public void setNombreMes(String nombreMes) {
        this.nombreMes = nombreMes;
    }

    public Boolean getRepetirConfiguracion() {
        return repetirConfiguracion;
    }

    public void setRepetirConfiguracion(Boolean repetirConfiguracion) {
        this.repetirConfiguracion = repetirConfiguracion;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual = estadoActual;
    }

    public List<DiaAgendaResumenResponseDto> getDias() {
        return dias;
    }

    public void setDias(List<DiaAgendaResumenResponseDto> dias) {
        this.dias = dias;
    }
}

