package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.AfectacionTurnoExcepcion;
import com.apiturnos.agenda.model.EstadoResolucionAfectacion;
import com.apiturnos.agenda.repository.AfectacionTurnoExcepcionRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.turno.service.DarDeBajaTurno;
import com.apiturnos.turno.service.ReprogramarTurno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ResolverAfectacionTurno {
    private final AfectacionTurnoExcepcionRepository repository;
    private final DarDeBajaTurno darDeBajaTurno;
    private final ReprogramarTurno reprogramarTurno;

    public ResolverAfectacionTurno(AfectacionTurnoExcepcionRepository repository,
                                  DarDeBajaTurno darDeBajaTurno,
                                  ReprogramarTurno reprogramarTurno) {
        this.repository = repository;
        this.darDeBajaTurno = darDeBajaTurno;
        this.reprogramarTurno = reprogramarTurno;
    }

    @Transactional
    public AfectacionTurnoExcepcion darDeBaja(Long profesionalId, Long afectacionId,
                                               String observacion, String usuario) {
        AfectacionTurnoExcepcion afectacion = pendiente(profesionalId, afectacionId);
        String motivo = afectacion.getExcepcionAgenda().getMotivo()
                + textoObservacion(observacion);
        darDeBajaTurno.ejecutar(profesionalId, afectacion.getTurno().getId(), motivo, usuario);
        return resolver(afectacion, EstadoResolucionAfectacion.DADO_DE_BAJA, observacion);
    }

    @Transactional
    public AfectacionTurnoExcepcion reprogramar(Long profesionalId, Long afectacionId,
                                                 Long nuevoDiaAgendaId, Instant nuevoInicio,
                                                 Instant nuevoFin, String observacion, String usuario) {
        if (nuevoDiaAgendaId == null || nuevoInicio == null || nuevoFin == null) {
            throw new EstadoInvalidoException("La nueva fecha y horario son obligatorios");
        }
        AfectacionTurnoExcepcion afectacion = pendiente(profesionalId, afectacionId);
        String motivo = "Reprogramación por excepción " + afectacion.getExcepcionAgenda().getId()
                + textoObservacion(observacion);
        reprogramarTurno.ejecutar(afectacion.getTurno().getId(), nuevoDiaAgendaId,
                nuevoInicio, nuevoFin, motivo, usuario);
        return resolver(afectacion, EstadoResolucionAfectacion.REPROGRAMADO, observacion);
    }

    private AfectacionTurnoExcepcion pendiente(Long profesionalId, Long afectacionId) {
        AfectacionTurnoExcepcion afectacion = repository
                .findByIdAndExcepcionAgendaProfesionalId(afectacionId, profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("AfectacionTurnoExcepcion", afectacionId));
        if (afectacion.getEstadoResolucion() != EstadoResolucionAfectacion.PENDIENTE) {
            throw new EstadoInvalidoException("El turno afectado ya fue resuelto");
        }
        return afectacion;
    }

    private AfectacionTurnoExcepcion resolver(AfectacionTurnoExcepcion afectacion,
                                               EstadoResolucionAfectacion estado, String observacion) {
        afectacion.setEstadoResolucion(estado);
        afectacion.setObservacion(observacion == null ? null : observacion.trim());
        afectacion.setResueltoEn(Instant.now());
        return repository.save(afectacion);
    }

    private String textoObservacion(String observacion) {
        return observacion == null || observacion.isBlank() ? "" : ". " + observacion.trim();
    }
}
