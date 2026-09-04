package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.CoincidenciaExcepcionAgendaResponseDto;
import com.apiturnos.agenda.dto.ExcepcionAgendaResponseDto;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.shared.exception.ExcepcionAgendaSuperpuestaException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DetectorCoincidenciasExcepcionAgenda {
    private final ExcepcionAgendaRepository repository;

    public DetectorCoincidenciasExcepcionAgenda(ExcepcionAgendaRepository repository) {
        this.repository = repository;
    }

    public void validar(Long profesionalId, ExcepcionAgenda candidata) {
        List<CoincidenciaExcepcionAgendaResponseDto> coincidencias = repository
                .findActivasIntersectandoRango(profesionalId, candidata.getFechaInicio(), candidata.getFechaFin())
                .stream()
                .filter(existente -> candidata.getId() == null || !candidata.getId().equals(existente.getId()))
                .map(existente -> coincidencia(candidata, existente))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (!coincidencias.isEmpty()) throw new ExcepcionAgendaSuperpuestaException(coincidencias);
    }

    private CoincidenciaExcepcionAgendaResponseDto coincidencia(
            ExcepcionAgenda candidata, ExcepcionAgenda existente) {
        List<LocalDate> fechas = fechasEfectivasComunes(candidata, existente);
        if (fechas.isEmpty() || !tiposIncompatibles(candidata, existente)) return null;
        return new CoincidenciaExcepcionAgendaResponseDto(
                ExcepcionAgendaResponseDto.from(existente), fechas,
                candidata.getTipo() == TipoExcepcion.HABILITACION_EXTRAORDINARIA
                        ? "MODIFICAR_EXISTENTE_PARA_HABILITAR"
                        : "MODIFICAR_EXISTENTE");
    }

    private boolean tiposIncompatibles(ExcepcionAgenda candidata, ExcepcionAgenda existente) {
        TipoExcepcion nuevo = candidata.getTipo();
        TipoExcepcion actual = existente.getTipo();
        if (nuevo == TipoExcepcion.HABILITACION_EXTRAORDINARIA) return true;
        // Los bloqueos superpuestos pueden coexistir. Si sólo son contiguos, se ofrece consolidarlos.
        if (nuevo.esBloqueoHorario() && actual.esBloqueoHorario()) {
            return intervalosSoloContiguos(candidata.obtenerIntervalos(), existente.obtenerIntervalos());
        }
        if (nuevo.esCierreDiaCompleto() || actual.esCierreDiaCompleto()) return true;
        if (nuevo == TipoExcepcion.MODIFICACION_HORARIO || actual == TipoExcepcion.MODIFICACION_HORARIO) {
            return intervalosSeSuperponen(candidata.obtenerIntervalos(), existente.obtenerIntervalos());
        }
        return intervalosSeSuperponen(candidata.obtenerIntervalos(), existente.obtenerIntervalos());
    }

    private boolean intervalosSeSuperponen(List<IntervaloHorario> a, List<IntervaloHorario> b) {
        if (a.isEmpty() || b.isEmpty()) return true;
        return a.stream().anyMatch(uno -> b.stream().anyMatch(otro ->
                !uno.fin().isBefore(otro.inicio()) && !otro.fin().isBefore(uno.inicio())));
    }

    private boolean intervalosSoloContiguos(List<IntervaloHorario> a, List<IntervaloHorario> b) {
        return a.stream().anyMatch(uno -> b.stream().anyMatch(otro ->
                uno.fin().equals(otro.inicio()) || otro.fin().equals(uno.inicio())));
    }

    private List<LocalDate> fechasEfectivasComunes(ExcepcionAgenda a, ExcepcionAgenda b) {
        LocalDate desde = a.getFechaInicio().isAfter(b.getFechaInicio()) ? a.getFechaInicio() : b.getFechaInicio();
        LocalDate hasta = a.getFechaFin().isBefore(b.getFechaFin()) ? a.getFechaFin() : b.getFechaFin();
        List<LocalDate> resultado = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            if (a.aplicaEn(fecha) && b.aplicaEn(fecha)) resultado.add(fecha);
        }
        return resultado;
    }
}
