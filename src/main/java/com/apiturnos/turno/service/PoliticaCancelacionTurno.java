package com.apiturnos.turno.service;

import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Único punto de decisión temporal para una cancelación ordinaria. */
@Component
public class PoliticaCancelacionTurno {

    private final Clock clock;

    public PoliticaCancelacionTurno(Clock clock) {
        this.clock = clock;
    }

    public TipoResolucionCancelacion resolver(Long turnoId, Instant inicioEstimado,
                                               Configuracion configuracion) {
        if (configuracion == null || configuracion.getUmbralCancelacionHoras() == null) {
            throw new EstadoInvalidoException(
                    "El Profesional del Turno " + turnoId + " no tiene configurado el umbral de cancelación");
        }
        return resolver(turnoId, inicioEstimado,
                Duration.ofHours(configuracion.getUmbralCancelacionHoras()));
    }

    TipoResolucionCancelacion resolver(Long turnoId, Instant inicioEstimado, Duration umbral) {
        if (inicioEstimado == null) {
            throw new EstadoInvalidoException("El Turno " + turnoId + " no tiene inicio estimado");
        }
        if (umbral == null || umbral.isNegative()) {
            throw new EstadoInvalidoException("El umbral de cancelación no puede ser negativo");
        }

        Instant ahora = clock.instant();
        if (!ahora.isBefore(inicioEstimado)) {
            throw new TurnoYaIniciadoException(turnoId, inicioEstimado, ahora);
        }

        Instant limite = inicioEstimado.minus(umbral);
        return ahora.isBefore(limite)
                ? TipoResolucionCancelacion.ELIMINACION_ANTICIPADA
                : TipoResolucionCancelacion.CANCELACION_CON_HISTORIAL;
    }
}
