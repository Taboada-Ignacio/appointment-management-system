package com.apiturnos.agenda.dto;

import java.util.List;

public record BajaMasivaAfectacionesRequestDto(List<Long> afectacionIds, String observacion) {
}
