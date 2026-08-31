package com.apiturnos.cliente.dto;

import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;

public class ClienteResumenDto {

    private Long id;
    private String nombre;
    private String apellido;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String email;
    private String telefono;
    private String estadoActual;

    public ClienteResumenDto() {
    }

    public ClienteResumenDto(Cliente cliente, String estadoActual) {
        if (cliente != null) {
            this.id = cliente.getId();
            this.nombre = cliente.getNombre();
            this.apellido = cliente.getApellido();
            this.tipoDocumento = cliente.getTipoDocumento();
            this.numeroDocumento = cliente.getNumeroDocumento();
            this.email = cliente.getEmail();
            this.telefono = cliente.getTelefono();
        }
        this.estadoActual = estadoActual;
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

    public String getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual = estadoActual;
    }
}

