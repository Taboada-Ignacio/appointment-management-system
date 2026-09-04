package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;

public class DiaAgendaResumenResponseDto {

    private Long id;

    @Schema(type = "string", example = "2026-08-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private DayOfWeek diaSemana;
    private String nombreDiaSemana;
    private int cantidadBrechas;
    private int cantidadTurnosAsignados;
    private List<TipoExcepcion> tiposExcepcion;
    private String estadoActual;

    public DiaAgendaResumenResponseDto() {
    }

    public DiaAgendaResumenResponseDto(DiaAgenda dia, int cantidadBrechas, String estadoActual) {
        this(dia, cantidadBrechas, 0, List.of(), estadoActual);
    }

    public DiaAgendaResumenResponseDto(DiaAgenda dia, int cantidadBrechas, int cantidadTurnosAsignados,
                                       List<TipoExcepcion> tiposExcepcion, String estadoActual) {
        if (dia != null) {
            this.id = dia.getId();
            this.fecha = dia.getFecha();
            if (dia.getFecha() != null) {
                this.diaSemana = dia.getFecha().getDayOfWeek();
                this.nombreDiaSemana = dia.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            }
        }
        this.cantidadBrechas = cantidadBrechas;
        this.cantidadTurnosAsignados = cantidadTurnosAsignados;
        this.tiposExcepcion = tiposExcepcion != null ? List.copyOf(tiposExcepcion) : List.of();
        this.estadoActual = estadoActual;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DayOfWeek diaSemana) {
        this.diaSemana = diaSemana;
    }

    public String getNombreDiaSemana() {
        return nombreDiaSemana;
    }

    public void setNombreDiaSemana(String nombreDiaSemana) {
        this.nombreDiaSemana = nombreDiaSemana;
    }

    public int getCantidadBrechas() {
        return cantidadBrechas;
    }

    public void setCantidadBrechas(int cantidadBrechas) {
        this.cantidadBrechas = cantidadBrechas;
    }

    public int getCantidadTurnosAsignados() {
        return cantidadTurnosAsignados;
    }

    public void setCantidadTurnosAsignados(int cantidadTurnosAsignados) {
        this.cantidadTurnosAsignados = cantidadTurnosAsignados;
    }

    public List<TipoExcepcion> getTiposExcepcion() {
        return tiposExcepcion;
    }

    public void setTiposExcepcion(List<TipoExcepcion> tiposExcepcion) {
        this.tiposExcepcion = tiposExcepcion != null ? List.copyOf(tiposExcepcion) : List.of();
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual = estadoActual;
    }
}

