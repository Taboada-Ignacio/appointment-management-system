package com.apiturnos.agenda.dto;

import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.service.SolicitudExcepcionAgenda;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ExcepcionAgendaRequestDto(
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        @NotNull(message = "El tipo de excepción es obligatorio") TipoExcepcion tipo,
        List<@Valid BrechaExcepcionRequestDto> brechas,
        @NotBlank(message = "El motivo es obligatorio") String motivo,
        List<LocalDate> fechasExcluidas,
        String previewToken,
        List<@Valid DecisionTurnoAfectadoRequestDto> decisiones) {

    public SolicitudExcepcionAgenda toSolicitud() {
        List<IntervaloHorario> intervalos = brechas == null
                ? List.of()
                : brechas.stream()
                    .map(b -> new IntervaloHorario(b.horaInicio(), b.horaFin()))
                    .toList();
        return new SolicitudExcepcionAgenda(
                fechaInicio, fechaFin, tipo,
                intervalos.isEmpty() ? null : intervalos.getFirst().inicio(),
                intervalos.isEmpty() ? null : intervalos.getLast().fin(),
                intervalos,
                fechasExcluidas == null ? List.of() : List.copyOf(fechasExcluidas),
                motivo);
    }
}
