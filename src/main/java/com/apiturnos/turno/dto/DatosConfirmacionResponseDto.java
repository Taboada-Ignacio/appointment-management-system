package com.apiturnos.turno.dto;

import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.turno.service.DatosConfirmacionTurnoManual;

import java.time.LocalDate;
import java.time.LocalTime;

public class DatosConfirmacionResponseDto {

    private Long clienteId;
    private String nombreCliente;
    private String apellidoCliente;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Long tipoAtencionId;
    private String tipoAtencion;

    public DatosConfirmacionResponseDto() {
    }

    public DatosConfirmacionResponseDto(DatosConfirmacionTurnoManual datos) {
        if (datos != null) {
            this.clienteId = datos.clienteId();
            this.nombreCliente = datos.nombreCliente();
            this.apellidoCliente = datos.apellidoCliente();
            this.tipoDocumento = datos.tipoDocumento();
            this.numeroDocumento = datos.numeroDocumento();
            this.fecha = datos.fecha();
            this.horaInicio = datos.horaInicio();
            this.horaFin = datos.horaFin();
            this.tipoAtencionId = datos.tipoAtencionId();
            this.tipoAtencion = datos.tipoAtencion();
        }
    }

    public DatosConfirmacionResponseDto(Long clienteId, String nombreCliente, String apellidoCliente,
                                       TipoDocumento tipoDocumento, String numeroDocumento,
                                       LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                       Long tipoAtencionId, String tipoAtencion) {
        this.clienteId = clienteId;
        this.nombreCliente = nombreCliente;
        this.apellidoCliente = apellidoCliente;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.tipoAtencionId = tipoAtencionId;
        this.tipoAtencion = tipoAtencion;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getApellidoCliente() {
        return apellidoCliente;
    }

    public void setApellidoCliente(String apellidoCliente) {
        this.apellidoCliente = apellidoCliente;
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

    public String getDni() {
        return numeroDocumento;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public Long getTipoAtencionId() {
        return tipoAtencionId;
    }

    public void setTipoAtencionId(Long tipoAtencionId) {
        this.tipoAtencionId = tipoAtencionId;
    }

    public String getTipoAtencion() {
        return tipoAtencion;
    }

    public void setTipoAtencion(String tipoAtencion) {
        this.tipoAtencion = tipoAtencion;
    }
}

