package com.apiturnos.cliente.dto;

import com.apiturnos.cliente.model.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClienteRequestDto {
    @NotBlank @Size(max = 100)
    private String nombre;
    @NotBlank @Size(max = 100)
    private String apellido;
    @NotNull
    private TipoDocumento tipoDocumento;
    @NotBlank @Size(max = 30)
    private String numeroDocumento;
    @NotBlank @Email @Size(max = 150)
    private String email;
    @NotBlank @Size(max = 50)
    private String telefono;
    private Boolean notificacionesHabilitadas;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Boolean getNotificacionesHabilitadas() { return notificacionesHabilitadas; }
    public void setNotificacionesHabilitadas(Boolean value) { this.notificacionesHabilitadas = value; }
}
