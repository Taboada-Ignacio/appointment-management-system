package com.apiturnos.agenda.dto;

public record ResultadoExcepcionAgendaResponseDto(
        ExcepcionAgendaResponseDto excepcion,
        ImpactoExcepcionAgendaResponseDto impacto) {
}
