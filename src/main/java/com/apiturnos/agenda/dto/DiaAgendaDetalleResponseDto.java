package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.DiaAgenda;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiaAgendaDetalleResponseDto {

    private Long id;
    private Long mesAgendaId;

    @Schema(type = "string", example = "2026-08-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private DayOfWeek diaSemana;
    private String nombreDiaSemana;
    private String estadoActual;
    private List<BrechaHorariaResponseDto> brechas = new ArrayList<>();

    public DiaAgendaDetalleResponseDto() {
    }

    public DiaAgendaDetalleResponseDto(DiaAgenda dia, String estadoActual, List<BrechaHorariaResponseDto> brechas) {
        if (dia != null) {
            this.id = dia.getId();
            this.mesAgendaId = dia.getMesAgenda() != null ? dia.getMesAgenda().getId() : null;
            this.fecha = dia.getFecha();
            if (dia.getFecha() != null) {
                this.diaSemana = dia.getFecha().getDayOfWeek();
                this.nombreDiaSemana = dia.getFecha().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
            }
        }
        this.estadoActual = estadoActual;
        this.brechas = brechas != null ? brechas : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMesAgendaId() {
        return mesAgendaId;
    }

    public void setMesAgendaId(Long mesAgendaId) {
        this.mesAgendaId = mesAgendaId;
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

    public String getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual = estadoActual;
    }

    public List<BrechaHorariaResponseDto> getBrechas() {
        return brechas;
    }

    public void setBrechas(List<BrechaHorariaResponseDto> brechas) {
        this.brechas = brechas;
    }
}

