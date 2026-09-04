package com.apiturnos.agenda.dto;

import java.time.LocalDate;
import java.util.List;

public record CoincidenciaExcepcionAgendaResponseDto(
        ExcepcionAgendaResponseDto excepcion,
        List<LocalDate> fechasCoincidentes,
        String accionSugerida) {
}
