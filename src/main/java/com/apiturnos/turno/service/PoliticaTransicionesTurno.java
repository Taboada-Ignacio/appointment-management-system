package com.apiturnos.turno.service;

import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.TransicionEstadoInvalidaException;
import com.apiturnos.turno.model.MotivoBajaTurno;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/** Politica explicita y unica del ciclo de vida de un turno. */
@Component
public class PoliticaTransicionesTurno {

    public static final String PENDIENTE_DE_APROBACION = "PENDIENTE_DE_APROBACION";
    public static final String ASIGNADO = "ASIGNADO";
    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String REPROGRAMADO = "REPROGRAMADO";
    public static final String CANCELADO = "CANCELADO";
    public static final String DADO_DE_BAJA = "DADO_DE_BAJA";
    public static final String ASISTIO = "COMPLETADO";
    public static final String AUSENTE = "NO_ASISTIO";

    private static final Set<String> ESTADOS_INICIALES = Set.of(
            ASIGNADO, PENDIENTE_DE_APROBACION);

    private static final Set<String> ESTADOS_TERMINALES = Set.of(
            CANCELADO, DADO_DE_BAJA, ASISTIO, AUSENTE);

    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            PENDIENTE_DE_APROBACION, Set.of(ASIGNADO, CANCELADO, DADO_DE_BAJA),
            ASIGNADO, Set.of(REPROGRAMADO, CANCELADO, DADO_DE_BAJA, ASISTIO, AUSENTE, CONFIRMADO),
            CONFIRMADO, Set.of(CANCELADO, DADO_DE_BAJA, ASISTIO, AUSENTE),
            REPROGRAMADO, Set.of(ASIGNADO)
    );

    public void validarEstadoInicial(String estado, Long turnoId) {
        if (!ESTADOS_INICIALES.contains(estado)) {
            throw new EstadoInvalidoException(
                    "Estado inicial invalido para Turno " + turnoId + ": " + estado);
        }
    }

    public void validarTransicion(Long turnoId, String estadoActual, String estadoDestino,
                                  MotivoBajaTurno motivo) {
        validarTransicion(turnoId, estadoActual, estadoDestino);
        validarMotivo(turnoId, estadoDestino, motivo);
    }

    public void validarTransicion(Long turnoId, String estadoActual, String estadoDestino) {
        Set<String> destinos = TRANSICIONES.get(estadoActual);
        if (destinos == null || !destinos.contains(estadoDestino)) {
            throw new TransicionEstadoInvalidaException(
                    estadoActual, estadoDestino, "TURNO", turnoId);
        }
    }

    public void validarMotivo(Long turnoId, String estadoDestino, MotivoBajaTurno motivo) {
        if (requiereMotivo(estadoDestino) && motivo == null) {
            throw new EstadoInvalidoException(
                    "La transicion del Turno " + turnoId + " a " + estadoDestino
                            + " requiere MotivoBajaTurno");
        }
    }

    public boolean esTerminal(String estado) {
        return ESTADOS_TERMINALES.contains(estado);
    }

    public boolean requiereMotivo(String estadoDestino) {
        return CANCELADO.equals(estadoDestino) || DADO_DE_BAJA.equals(estadoDestino);
    }

    public Set<String> destinosPermitidos(String estadoActual) {
        return TRANSICIONES.getOrDefault(estadoActual, Set.of());
    }
}
