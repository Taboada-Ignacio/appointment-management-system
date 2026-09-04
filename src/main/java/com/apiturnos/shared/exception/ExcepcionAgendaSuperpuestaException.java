package com.apiturnos.shared.exception;

import com.apiturnos.agenda.dto.CoincidenciaExcepcionAgendaResponseDto;
import java.util.List;

public class ExcepcionAgendaSuperpuestaException extends RuntimeException {
    private final List<CoincidenciaExcepcionAgendaResponseDto> coincidencias;

    public ExcepcionAgendaSuperpuestaException(List<CoincidenciaExcepcionAgendaResponseDto> coincidencias) {
        super("La nueva excepción coincide con una o más excepciones activas");
        this.coincidencias = List.copyOf(coincidencias);
    }

    public List<CoincidenciaExcepcionAgendaResponseDto> getCoincidencias() {
        return coincidencias;
    }
}
