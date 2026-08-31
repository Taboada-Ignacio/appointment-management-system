package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.shared.exception.ExcepcionAgendaInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorExcepcionAgendaUnitTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 15);

    private final ValidadorExcepcionAgenda validador = new ValidadorExcepcionAgenda();

    @Test
    void rechazaUnaSolicitudNula() {
        assertThatThrownBy(() -> validador.validar(null))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("obligatoria");
    }

    @ParameterizedTest
    @MethodSource("solicitudesSinRangoCompleto")
    void exigeAmbasFechasDelRango(SolicitudExcepcionAgenda solicitud) {
        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("rango de fechas");
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        SolicitudExcepcionAgenda solicitud = solicitud(
                FECHA.plusDays(1), FECHA, TipoExcepcion.VACACIONES,
                null, null, "Descanso");

        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("fecha de inicio");
    }

    @Test
    void aceptaUnaExcepcionDeUnaSolaFecha() {
        SolicitudExcepcionAgenda solicitud = solicitud(
                FECHA, FECHA, TipoExcepcion.DIA_NO_LABORABLE,
                null, null, "Jornada no laborable");

        assertThatCode(() -> validador.validar(solicitud)).doesNotThrowAnyException();
    }

    @Test
    void exigeElTipoDeExcepcion() {
        SolicitudExcepcionAgenda solicitud = solicitud(
                FECHA, FECHA, null, null, null, "Motivo valido");

        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("tipo");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void exigeUnMotivoNoVacio(String motivo) {
        SolicitudExcepcionAgenda solicitud = solicitud(
                FECHA, FECHA, TipoExcepcion.VACACIONES,
                null, null, motivo);

        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("motivo");
    }

    @ParameterizedTest
    @MethodSource("franjasIncompletas")
    void exigeInformarLasDosHorasJuntas(LocalTime horaInicio, LocalTime horaFin) {
        SolicitudExcepcionAgenda solicitud = new SolicitudExcepcionAgenda(
                FECHA, FECHA, TipoExcepcion.BLOQUEO_HORARIO,
                horaInicio, horaFin, List.of(), "Bloqueo");

        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("deben informarse juntas");
    }

    @ParameterizedTest
    @EnumSource(value = TipoExcepcion.class, names = {
            "BLOQUEO_HORARIO",
            "HABILITACION_EXTRAORDINARIA",
            "MODIFICACION_HORARIO",
            "EXCEPCION_HORARIA"
    })
    void losTiposHorariosExigenUnaFranja(TipoExcepcion tipo) {
        SolicitudExcepcionAgenda solicitud = solicitud(
                FECHA, FECHA, tipo, null, null, "Cambio horario");

        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("requiere una franja horaria");
    }

    @ParameterizedTest
    @MethodSource("franjasNoCrecientes")
    void rechazaUnaFranjaConHorasIgualesOInvertidas(
            LocalTime horaInicio,
            LocalTime horaFin) {

        SolicitudExcepcionAgenda solicitud = new SolicitudExcepcionAgenda(
                FECHA, FECHA, TipoExcepcion.MODIFICACION_HORARIO,
                horaInicio, horaFin, List.of(), "Horario especial");

        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("hora de inicio");
    }

    @ParameterizedTest
    @EnumSource(value = TipoExcepcion.class, names = {
            "DIA_NO_LABORABLE",
            "VACACIONES",
            "FERIADO",
            "DIA_DADO_DE_BAJA",
            "OTRO"
    })
    void losTiposDeDiaCompletoNoAdmitenHoras(TipoExcepcion tipo) {
        SolicitudExcepcionAgenda solicitud = solicitud(
                FECHA, FECHA, tipo,
                LocalTime.of(8, 0), LocalTime.of(12, 0), "Cierre");

        assertThatThrownBy(() -> validador.validar(solicitud))
                .isInstanceOf(ExcepcionAgendaInvalidaException.class)
                .hasMessageContaining("no admite una franja horaria");
    }

    @Test
    void aceptaModificacionConMultiplesBrechas() {
        SolicitudExcepcionAgenda solicitud = new SolicitudExcepcionAgenda(
                FECHA, FECHA, TipoExcepcion.MODIFICACION_HORARIO,
                List.of(
                        new IntervaloHorario(LocalTime.of(8, 30), LocalTime.of(12, 0)),
                        new IntervaloHorario(LocalTime.of(14, 0), LocalTime.of(18, 0))),
                "Horario partido");

        assertThatCode(() -> validador.validar(solicitud)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(TipoExcepcion.class)
    void aceptaUnaSolicitudValidaParaCadaTipoSoportado(TipoExcepcion tipo) {
        LocalTime horaInicio = tipo.requiereHorario() ? LocalTime.of(8, 0) : null;
        LocalTime horaFin = tipo.requiereHorario() ? LocalTime.of(12, 0) : null;
        SolicitudExcepcionAgenda solicitud = solicitud(
                FECHA, FECHA.plusDays(2), tipo,
                horaInicio, horaFin, "Motivo valido");

        assertThatCode(() -> validador.validar(solicitud)).doesNotThrowAnyException();
    }

    private static Stream<SolicitudExcepcionAgenda> solicitudesSinRangoCompleto() {
        return Stream.of(
                solicitud(null, FECHA, TipoExcepcion.VACACIONES,
                        null, null, "Descanso"),
                solicitud(FECHA, null, TipoExcepcion.VACACIONES,
                        null, null, "Descanso"));
    }

    private static Stream<Arguments> franjasIncompletas() {
        return Stream.of(
                Arguments.of(LocalTime.of(8, 0), null),
                Arguments.of(null, LocalTime.of(12, 0)));
    }

    private static Stream<Arguments> franjasNoCrecientes() {
        return Stream.of(
                Arguments.of(LocalTime.of(10, 0), LocalTime.of(10, 0)),
                Arguments.of(LocalTime.of(11, 0), LocalTime.of(10, 0)));
    }

    private static SolicitudExcepcionAgenda solicitud(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            TipoExcepcion tipo,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo) {

        return new SolicitudExcepcionAgenda(
                fechaInicio, fechaFin, tipo, horaInicio, horaFin, motivo);
    }
}
