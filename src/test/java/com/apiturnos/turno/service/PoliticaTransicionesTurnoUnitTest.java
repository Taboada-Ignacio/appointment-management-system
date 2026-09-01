package com.apiturnos.turno.service;

import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.TransicionEstadoInvalidaException;
import com.apiturnos.turno.model.MotivoBajaTurno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaTransicionesTurnoUnitTest {

    private final PoliticaTransicionesTurno politica = new PoliticaTransicionesTurno();

    @ParameterizedTest
    @MethodSource("transicionesValidas")
    void aceptaMatrizValida(String origen, String destino, boolean requiereMotivo) {
        MotivoBajaTurno motivo = requiereMotivo ? motivo() : null;

        assertThatCode(() -> politica.validarTransicion(42L, origen, destino, motivo))
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> transicionesValidas() {
        return Stream.of(
                Arguments.of("PENDIENTE_DE_APROBACION", "ASIGNADO", false),
                Arguments.of("PENDIENTE_DE_APROBACION", "CANCELADO", true),
                Arguments.of("PENDIENTE_DE_APROBACION", "DADO_DE_BAJA", true),
                Arguments.of("ASIGNADO", "REPROGRAMADO", false),
                Arguments.of("ASIGNADO", "CANCELADO", true),
                Arguments.of("ASIGNADO", "DADO_DE_BAJA", true),
                Arguments.of("ASIGNADO", "COMPLETADO", false),
                Arguments.of("ASIGNADO", "NO_ASISTIO", false),
                Arguments.of("ASIGNADO", "CONFIRMADO", false),
                Arguments.of("CONFIRMADO", "CANCELADO", true),
                Arguments.of("CONFIRMADO", "DADO_DE_BAJA", true),
                Arguments.of("CONFIRMADO", "COMPLETADO", false),
                Arguments.of("CONFIRMADO", "NO_ASISTIO", false),
                Arguments.of("REPROGRAMADO", "ASIGNADO", false));
    }

    @ParameterizedTest
    @MethodSource("transicionesTerminalesInvalidas")
    void estadosTerminalesNoTienenSalidas(String terminal, String destino) {
        assertThatThrownBy(() -> politica.validarTransicion(99L, terminal, destino, null))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .satisfies(ex -> assertThat(((TransicionEstadoInvalidaException) ex).getEntidadId())
                        .isEqualTo(99L));
    }

    static Stream<Arguments> transicionesTerminalesInvalidas() {
        return Stream.of(
                Arguments.of("CANCELADO", "ASIGNADO"),
                Arguments.of("DADO_DE_BAJA", "REPROGRAMADO"),
                Arguments.of("COMPLETADO", "CANCELADO"),
                Arguments.of("NO_ASISTIO", "ASIGNADO"));
    }

    @Test
    void reprogramadoSoloPuedeVolverAAsignado() {
        assertThat(politica.destinosPermitidos("REPROGRAMADO"))
                .containsExactly("ASIGNADO");
    }

    @Test
    void confirmadoNoPuedeReprogramarse() {
        assertThatThrownBy(() -> politica.validarTransicion(
                8L, "CONFIRMADO", "REPROGRAMADO", null))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    void cancelacionYBajaExigenMotivo() {
        assertThatThrownBy(() -> politica.validarTransicion(
                7L, "ASIGNADO", "CANCELADO", null))
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessageContaining("MotivoBajaTurno");
    }

    @Test
    void soloPermiteEstadosInicialesAcordados() {
        assertThatCode(() -> politica.validarEstadoInicial("ASIGNADO", 1L))
                .doesNotThrowAnyException();
        assertThatCode(() -> politica.validarEstadoInicial("PENDIENTE_DE_APROBACION", 2L))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> politica.validarEstadoInicial("CANCELADO", 3L))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    private MotivoBajaTurno motivo() {
        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo("Motivo de prueba");
        return motivo;
    }
}
