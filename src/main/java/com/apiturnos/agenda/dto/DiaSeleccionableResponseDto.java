package com.apiturnos.agenda.dto;

import java.time.LocalDate;

public class DiaSeleccionableResponseDto {

    private Long diaAgendaId;
    private LocalDate fecha;
    private String estado;
    private String nombreDiaSemana;
    private boolean seleccionable;
    private String mensaje;

    public DiaSeleccionableResponseDto() {
    }

    public DiaSeleccionableResponseDto(Long diaAgendaId, LocalDate fecha, String estado,
                                       String nombreDiaSemana, boolean seleccionable, String mensaje) {
        this.diaAgendaId = diaAgendaId;
        this.fecha = fecha;
        this.estado = estado;
        this.nombreDiaSemana = nombreDiaSemana;
        this.seleccionable = seleccionable;
        this.mensaje = mensaje;
    }

    public Long getDiaAgendaId() {
        return diaAgendaId;
    }

    public void setDiaAgendaId(Long diaAgendaId) {
        this.diaAgendaId = diaAgendaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNombreDiaSemana() {
        return nombreDiaSemana;
    }

    public void setNombreDiaSemana(String nombreDiaSemana) {
        this.nombreDiaSemana = nombreDiaSemana;
    }

    public boolean isSeleccionable() {
        return seleccionable;
    }

    public void setSeleccionable(boolean seleccionable) {
        this.seleccionable = seleccionable;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}

