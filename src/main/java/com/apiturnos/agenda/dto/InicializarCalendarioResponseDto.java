package com.apiturnos.agenda.dto;

import java.util.List;

public record InicializarCalendarioResponseDto(
        boolean completado,
        List<Integer> agendasAnuales,
        List<MesConfiguradoDto> mesesConfigurados,
        int diasLaborablesPorSemana,
        boolean repetidoAlMesSiguiente) {

    public record MesConfiguradoDto(
            Long id,
            Integer anio,
            Integer nroMes,
            String estado,
            int diasActivos,
            int diasInactivos) {
    }
}
