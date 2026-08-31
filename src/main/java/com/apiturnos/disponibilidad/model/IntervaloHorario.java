package com.apiturnos.disponibilidad.model;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Intervalo horario semiabierto: incluye {@code inicio} y excluye {@code fin}.
 */
public record IntervaloHorario(LocalTime inicio, LocalTime fin) {

    public IntervaloHorario {
        Objects.requireNonNull(inicio, "La hora de inicio es obligatoria");
        Objects.requireNonNull(fin, "La hora de fin es obligatoria");
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
    }

    /**
     * Indica si este intervalo contiene completamente al intervalo indicado.
     */
    public boolean contiene(IntervaloHorario otro) {
        Objects.requireNonNull(otro, "El intervalo a comprobar es obligatorio");
        return !otro.inicio().isBefore(inicio) && !otro.fin().isAfter(fin);
    }

    /**
     * Indica si ambos intervalos comparten tiempo. Los extremos contiguos no se solapan.
     */
    public boolean seSolapaCon(IntervaloHorario otro) {
        Objects.requireNonNull(otro, "El intervalo a comprobar es obligatorio");
        return inicio.isBefore(otro.fin()) && otro.inicio().isBefore(fin);
    }
}
