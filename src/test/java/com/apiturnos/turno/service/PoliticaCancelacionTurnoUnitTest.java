package com.apiturnos.turno.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaCancelacionTurnoUnitTest {

    private static final Instant AHORA = Instant.parse("2035-06-10T18:00:00Z");
    private final PoliticaCancelacionTurno politica = new PoliticaCancelacionTurno(
            Clock.fixed(AHORA, ZoneId.of("America/Argentina/Buenos_Aires")));

    @Test
    void antesDelLimiteResuelveEliminacionAnticipada() {
        assertThat(politica.resolver(1L, AHORA.plus(Duration.ofHours(25)), Duration.ofHours(24)))
                .isEqualTo(TipoResolucionCancelacion.ELIMINACION_ANTICIPADA);
    }

    @Test
    void igualdadExactaConElLimiteResuelveCancelacionConHistorial() {
        assertThat(politica.resolver(1L, AHORA.plus(Duration.ofHours(24)), Duration.ofHours(24)))
                .isEqualTo(TipoResolucionCancelacion.CANCELACION_CON_HISTORIAL);
    }

    @Test
    void dentroDelUmbralResuelveCancelacionConHistorial() {
        assertThat(politica.resolver(1L, AHORA.plus(Duration.ofMinutes(30)), Duration.ofHours(24)))
                .isEqualTo(TipoResolucionCancelacion.CANCELACION_CON_HISTORIAL);
    }

    @Test
    void turnoExactamenteEnSuInicioEsRechazado() {
        assertThatThrownBy(() -> politica.resolver(1L, AHORA, Duration.ofHours(24)))
                .isInstanceOf(TurnoYaIniciadoException.class);
    }

    @Test
    void turnoPasadoEsRechazado() {
        assertThatThrownBy(() -> politica.resolver(
                1L, AHORA.minus(Duration.ofMinutes(1)), Duration.ofHours(24)))
                .isInstanceOf(TurnoYaIniciadoException.class);
    }
}
