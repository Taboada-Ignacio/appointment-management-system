package com.apiturnos.turno.service;

import com.apiturnos.cliente.model.TipoDocumento;

import java.time.LocalDate;
import java.time.LocalTime;

public record DatosConfirmacionTurnoManual(
        Long clienteId,
        String nombreCliente,
        String apellidoCliente,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long tipoAtencionId,
        String tipoAtencion) {
}
