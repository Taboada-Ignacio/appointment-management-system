package com.apiturnos.turno.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public class CrearTurnoManualRequestDto {

    @NotNull(message = "El ID del cliente es obligatorio")
    @JsonAlias({"idCliente", "clienteId"})
    @JsonProperty("clienteId")
    private Long clienteId;

    @JsonAlias({"idDiaAgenda", "diaAgendaId"})
    @JsonProperty("diaAgendaId")
    private Long diaAgendaId;

    private LocalDate fecha;

    @NotNull(message = "El ID del tipo de atención es obligatorio")
    @JsonAlias({"idTipoAtencion", "tipoAtencionId"})
    @JsonProperty("tipoAtencionId")
    private Long tipoAtencionId;

    @NotNull(message = "El inicio estimado es obligatorio")
    private Instant inicioEstimado;

    @NotNull(message = "El fin estimado es obligatorio")
    private Instant finEstimado;

    private boolean confirmarAdvertencias = false;

    private String observaciones;

    public CrearTurnoManualRequestDto() {
    }

    public CrearTurnoManualRequestDto(Long clienteId, Long diaAgendaId, Long tipoAtencionId,
                                      Instant inicioEstimado, Instant finEstimado,
                                      boolean confirmarAdvertencias, String observaciones) {
        this.clienteId = clienteId;
        this.diaAgendaId = diaAgendaId;
        this.tipoAtencionId = tipoAtencionId;
        this.inicioEstimado = inicioEstimado;
        this.finEstimado = finEstimado;
        this.confirmarAdvertencias = confirmarAdvertencias;
        this.observaciones = observaciones;
    }

    public CrearTurnoManualRequestDto(Long clienteId, LocalDate fecha, Long tipoAtencionId,
                                      Instant inicioEstimado, Instant finEstimado,
                                      boolean confirmarAdvertencias, String observaciones) {
        this.clienteId = clienteId;
        this.fecha = fecha;
        this.tipoAtencionId = tipoAtencionId;
        this.inicioEstimado = inicioEstimado;
        this.finEstimado = finEstimado;
        this.confirmarAdvertencias = confirmarAdvertencias;
        this.observaciones = observaciones;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getIdCliente() {
        return clienteId;
    }

    public void setIdCliente(Long idCliente) {
        this.clienteId = idCliente;
    }

    public Long getDiaAgendaId() {
        return diaAgendaId;
    }

    public void setDiaAgendaId(Long diaAgendaId) {
        this.diaAgendaId = diaAgendaId;
    }

    public Long getIdDiaAgenda() {
        return diaAgendaId;
    }

    public void setIdDiaAgenda(Long idDiaAgenda) {
        this.diaAgendaId = idDiaAgenda;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getTipoAtencionId() {
        return tipoAtencionId;
    }

    public void setTipoAtencionId(Long tipoAtencionId) {
        this.tipoAtencionId = tipoAtencionId;
    }

    public Long getIdTipoAtencion() {
        return tipoAtencionId;
    }

    public void setIdTipoAtencion(Long idTipoAtencion) {
        this.tipoAtencionId = idTipoAtencion;
    }

    public Instant getInicioEstimado() {
        return inicioEstimado;
    }

    public void setInicioEstimado(Instant inicioEstimado) {
        this.inicioEstimado = inicioEstimado;
    }

    public Instant getFinEstimado() {
        return finEstimado;
    }

    public void setFinEstimado(Instant finEstimado) {
        this.finEstimado = finEstimado;
    }

    public boolean isConfirmarAdvertencias() {
        return confirmarAdvertencias;
    }

    public void setConfirmarAdvertencias(boolean confirmarAdvertencias) {
        this.confirmarAdvertencias = confirmarAdvertencias;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

