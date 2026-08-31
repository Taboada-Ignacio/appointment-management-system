package com.apiturnos.cliente.dto;

import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;

import java.time.Instant;

public class ClienteDetalleDto {

    private Long id;
    private Long profesionalId;
    private String nombre;
    private String apellido;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String email;
    private String telefono;
    private Boolean notificacionesHabilitadas;
    private String estadoActual;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public ClienteDetalleDto() {
    }

    public ClienteDetalleDto(Cliente cliente, String estadoActual) {
        if (cliente != null) {
            this.id = cliente.getId();
            this.profesionalId = cliente.getProfesional() != null ? cliente.getProfesional().getId() : null;
            this.nombre = cliente.getNombre();
            this.apellido = cliente.getApellido();
            this.tipoDocumento = cliente.getTipoDocumento();
            this.numeroDocumento = cliente.getNumeroDocumento();
            this.email = cliente.getEmail();
            this.telefono = cliente.getTelefono();
            this.notificacionesHabilitadas = cliente.getNotificacionesHabilitadas();
            this.creadoEn = cliente.getCreadoEn();
            this.actualizadoEn = cliente.getActualizadoEn();
        }
        this.estadoActual = estadoActual;
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
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

    public Boolean getNotificacionesHabilitadas() {
        return notificacionesHabilitadas;
    }

    public void setNotificacionesHabilitadas(Boolean notificacionesHabilitadas) {
        this.notificacionesHabilitadas = notificacionesHabilitadas;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual = estadoActual;
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

