package com.apiturnos.disponibilidad.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.turno.model.Turno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorTurnoAfectadoPorExcepcionUnitTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 15);

    private final DetectorTurnoAfectadoPorExcepcion detector =
            new DetectorTurnoAfectadoPorExcepcion("UTC");

    @Test
    void unTurnoDentroDelBloqueoQuedaAfectado() {
        Turno turno = turno("09:00", "09:30");
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, "09:00", "10:00");

        assertThat(detector.quedaAfectado(turno, bloqueo, List.of())).isTrue();
    }

    @Test
    void unTurnoFueraDelBloqueoPermaneceValido() {
        Turno turno = turno("11:00", "11:30");
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, "09:00", "10:00");

        assertThat(detector.quedaAfectado(turno, bloqueo, List.of())).isFalse();
    }

    @ParameterizedTest
    @MethodSource("turnosContiguosAlBloqueo")
    void losExtremosContiguosNoSeSolapanEnIntervalosSemiabiertos(
            String inicioTurno,
            String finTurno) {

        Turno turno = turno(inicioTurno, finTurno);
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, "09:00", "10:00");

        assertThat(detector.quedaAfectado(turno, bloqueo, List.of())).isFalse();
    }

    @ParameterizedTest
    @MethodSource("turnosConSolapamientoParcial")
    void detectaSolapamientoUsandoElIntervaloCompletoDelTurno(
            String inicioTurno,
            String finTurno) {

        Turno turno = turno(inicioTurno, finTurno);
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, "09:00", "10:00");

        assertThat(detector.quedaAfectado(turno, bloqueo, List.of())).isTrue();
    }

    @ParameterizedTest
    @MethodSource("turnosValidosTrasModificacion")
    void modificacionHorariaConservaTurnosCompletamenteContenidos(
            String inicioTurno,
            String finTurno) {

        Turno turno = turno(inicioTurno, finTurno);
        ExcepcionAgenda modificacion = excepcion(
                TipoExcepcion.MODIFICACION_HORARIO, "10:00", "15:00");
        List<IntervaloHorario> disponibilidad = List.of(intervalo("10:00", "15:00"));

        assertThat(detector.quedaAfectado(turno, modificacion, disponibilidad)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("turnosNoContenidosTrasModificacion")
    void modificacionHorariaAfectaTurnosQueNoQuedanCompletamenteContenidos(
            String inicioTurno,
            String finTurno) {

        Turno turno = turno(inicioTurno, finTurno);
        ExcepcionAgenda modificacion = excepcion(
                TipoExcepcion.MODIFICACION_HORARIO, "10:00", "15:00");
        List<IntervaloHorario> disponibilidad = List.of(intervalo("10:00", "15:00"));

        assertThat(detector.quedaAfectado(turno, modificacion, disponibilidad)).isTrue();
    }

    @Test
    void modificacionConMultiplesBrechasConservaTurnosEnCualquieraDeEllas() {
        ExcepcionAgenda modificacion = new ExcepcionAgenda();
        modificacion.setTipo(TipoExcepcion.MODIFICACION_HORARIO);
        modificacion.setActiva(true);
        modificacion.agregarBrecha(LocalTime.of(8, 30), LocalTime.of(12, 0));
        modificacion.agregarBrecha(LocalTime.of(14, 0), LocalTime.of(18, 0));

        List<IntervaloHorario> disponibilidad = List.of(
                intervalo("08:30", "12:00"),
                intervalo("14:00", "18:00"));

        Turno turnoManana = turno("09:00", "09:30");
        Turno turnoMediodia = turno("12:30", "13:00");
        Turno turnoTarde = turno("15:00", "15:30");

        assertThat(detector.quedaAfectado(turnoManana, modificacion, disponibilidad)).isFalse();
        assertThat(detector.quedaAfectado(turnoMediodia, modificacion, disponibilidad)).isTrue();
        assertThat(detector.quedaAfectado(turnoTarde, modificacion, disponibilidad)).isFalse();
    }

    @Test
    void detectorFuncionaConZonaHorariaArgentina() {
        DetectorTurnoAfectadoPorExcepcion detectorBsAs =
                new DetectorTurnoAfectadoPorExcepcion("America/Argentina/Buenos_Aires");

        DiaAgenda dia = new DiaAgenda();
        dia.setFecha(FECHA);

        // En America/Argentina/Buenos_Aires (UTC-3), las 09:00 locales equivalen a las 12:00 UTC
        ZoneId zoneBsAs = ZoneId.of("America/Argentina/Buenos_Aires");
        Turno turno = new Turno();
        turno.setDiaAgenda(dia);
        turno.setInicioEstimado(ZonedDateTime.of(FECHA, LocalTime.of(9, 0), zoneBsAs).toInstant());
        turno.setFinEstimado(ZonedDateTime.of(FECHA, LocalTime.of(9, 30), zoneBsAs).toInstant());

        ExcepcionAgenda bloqueo = excepcion(TipoExcepcion.BLOQUEO_HORARIO, "09:00", "10:00");

        assertThat(detectorBsAs.quedaAfectado(turno, bloqueo, List.of())).isTrue();
    }

    @Test
    void habilitacionExtraordinariaNuncaInvalidaUnTurnoExistente() {
        Turno turno = turno("09:00", "09:30");
        ExcepcionAgenda habilitacion = excepcion(
                TipoExcepcion.HABILITACION_EXTRAORDINARIA, "18:00", "21:00");

        assertThat(detector.quedaAfectado(
                turno,
                habilitacion,
                List.of(intervalo("18:00", "21:00"))))
                .isFalse();
    }

    @Test
    void cierreDeDiaCompletoAfectaCualquierTurnoDeLaFecha() {
        Turno turno = turno("11:00", "11:30");
        ExcepcionAgenda vacaciones = excepcion(TipoExcepcion.VACACIONES, null, null);

        assertThat(detector.quedaAfectado(turno, vacaciones, List.of())).isTrue();
    }

    private static Stream<Arguments> turnosContiguosAlBloqueo() {
        return Stream.of(
                Arguments.of("08:30", "09:00"),
                Arguments.of("10:00", "10:30"));
    }

    private static Stream<Arguments> turnosConSolapamientoParcial() {
        return Stream.of(
                Arguments.of("08:30", "09:30"),
                Arguments.of("09:30", "10:30"));
    }

    private static Stream<Arguments> turnosValidosTrasModificacion() {
        return Stream.of(
                Arguments.of("10:00", "15:00"),
                Arguments.of("11:00", "11:30"));
    }

    private static Stream<Arguments> turnosNoContenidosTrasModificacion() {
        return Stream.of(
                Arguments.of("09:30", "10:30"),
                Arguments.of("14:30", "15:30"));
    }

    private static Turno turno(String horaInicio, String horaFin) {
        DiaAgenda dia = new DiaAgenda();
        dia.setFecha(FECHA);

        Turno turno = new Turno();
        turno.setDiaAgenda(dia);
        turno.setInicioEstimado(instante(horaInicio));
        turno.setFinEstimado(instante(horaFin));
        return turno;
    }

    private static java.time.Instant instante(String hora) {
        return LocalDateTime.of(FECHA, LocalTime.parse(hora)).toInstant(ZoneOffset.UTC);
    }

    private static ExcepcionAgenda excepcion(
            TipoExcepcion tipo,
            String horaInicio,
            String horaFin) {

        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setTipo(tipo);
        excepcion.setActiva(true);
        if (horaInicio != null) {
            excepcion.setHoraInicio(LocalTime.parse(horaInicio));
        }
        if (horaFin != null) {
            excepcion.setHoraFin(LocalTime.parse(horaFin));
        }
        return excepcion;
    }

    private static IntervaloHorario intervalo(String inicio, String fin) {
        return new IntervaloHorario(LocalTime.parse(inicio), LocalTime.parse(fin));
    }
}
