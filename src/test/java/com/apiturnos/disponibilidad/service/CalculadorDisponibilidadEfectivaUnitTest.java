package com.apiturnos.disponibilidad.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadorDisponibilidadEfectivaUnitTest {

    private final CalculadorDisponibilidadEfectiva calculador =
            new CalculadorDisponibilidadEfectiva();

    @Test
    void diaNoLaborableEliminaTodaLaDisponibilidad() {
        List<IntervaloHorario> resultado = calculador.calcular(
                List.of(intervalo("08:00", "12:00")),
                List.of(excepcion(TipoExcepcion.DIA_NO_LABORABLE, null, null)));

        assertThat(resultado).isEmpty();
    }

    @Test
    void vacacionesEliminanTodaLaDisponibilidadDeCadaFechaEnQueSonAplicables() {
        ExcepcionAgenda vacaciones = excepcion(TipoExcepcion.VACACIONES, null, null);
        List<IntervaloHorario> base = List.of(
                intervalo("08:00", "12:00"),
                intervalo("16:00", "20:00"));

        assertThat(calculador.calcular(base, List.of(vacaciones))).isEmpty();
        assertThat(calculador.calcular(base, List.of(vacaciones))).isEmpty();
    }

    @Test
    void bloqueoRestaSolamenteLaFranjaIntersectada() {
        List<IntervaloHorario> resultado = calculador.calcular(
                List.of(intervalo("08:00", "12:00")),
                List.of(excepcion(TipoExcepcion.BLOQUEO_HORARIO, "08:00", "10:00")));

        assertThat(resultado).containsExactly(intervalo("10:00", "12:00"));
    }

    @Test
    void bloqueoParcialDivideUnaBrechaEnDosIntervalos() {
        List<IntervaloHorario> resultado = calculador.calcular(
                List.of(intervalo("08:00", "12:00")),
                List.of(excepcion(TipoExcepcion.BLOQUEO_HORARIO, "10:00", "11:00")));

        assertThat(resultado).containsExactly(
                intervalo("08:00", "10:00"),
                intervalo("11:00", "12:00"));
    }

    @Test
    void habilitacionExtraordinariaAgregaYNormalizaDisponibilidad() {
        List<IntervaloHorario> resultado = calculador.calcular(
                List.of(intervalo("08:00", "12:00")),
                List.of(
                        excepcion(TipoExcepcion.HABILITACION_EXTRAORDINARIA, "12:00", "14:00"),
                        excepcion(TipoExcepcion.HABILITACION_EXTRAORDINARIA, "18:00", "21:00")));

        assertThat(resultado).containsExactly(
                intervalo("08:00", "14:00"),
                intervalo("18:00", "21:00"));
    }

    @Test
    void habilitacionExtraordinariaFuncionaConDiaBaseInactivo() {
        List<IntervaloHorario> resultado = calculador.calcular(
                List.of(),
                List.of(excepcion(
                        TipoExcepcion.HABILITACION_EXTRAORDINARIA, "18:00", "21:00")));

        assertThat(resultado).containsExactly(intervalo("18:00", "21:00"));
    }

    @Test
    void modificacionHorariaReemplazaLaDisponibilidadBase() {
        List<IntervaloHorario> resultado = calculador.calcular(
                List.of(
                        intervalo("08:00", "12:00"),
                        intervalo("16:00", "20:00")),
                List.of(excepcion(TipoExcepcion.MODIFICACION_HORARIO, "10:00", "15:00")));

        assertThat(resultado).containsExactly(intervalo("10:00", "15:00"));
    }

    @Test
    void modificacionHorariaConMultiplesBrechasReemplazaLaDisponibilidadBase() {
        ExcepcionAgenda modificacion = new ExcepcionAgenda();
        modificacion.setTipo(TipoExcepcion.MODIFICACION_HORARIO);
        modificacion.setActiva(true);
        modificacion.agregarBrecha(LocalTime.of(8, 30), LocalTime.of(11, 30));
        modificacion.agregarBrecha(LocalTime.of(14, 0), LocalTime.of(17, 30));

        List<IntervaloHorario> resultado = calculador.calcular(
                List.of(intervalo("08:00", "20:00")),
                List.of(modificacion));

        assertThat(resultado).containsExactly(
                intervalo("08:30", "11:30"),
                intervalo("14:00", "17:30"));
    }

    @Test
    void calcularNoModificaLaConfiguracionBase() {
        List<IntervaloHorario> base = new ArrayList<>(List.of(
                intervalo("08:00", "12:00"),
                intervalo("16:00", "20:00")));
        List<IntervaloHorario> copia = List.copyOf(base);

        calculador.calcular(
                base,
                List.of(excepcion(TipoExcepcion.BLOQUEO_HORARIO, "09:00", "10:00")));

        assertThat(base).containsExactlyElementsOf(copia);
    }

    @Test
    void excepcionesSolapadasRespetanPipelineDeAplicacionSinDependerDelOrden() {
        // Pipeline: Base (08-12) -> Modificación (none) -> Bloqueo (11-13 resta -> 08-11) -> Habilitación (10-14 une -> 08-14)
        ExcepcionAgenda habilitacion = excepcion(
                TipoExcepcion.HABILITACION_EXTRAORDINARIA, "10:00", "14:00");
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, "11:00", "13:00");
        List<IntervaloHorario> base = List.of(intervalo("08:00", "12:00"));

        List<IntervaloHorario> resultadoPrimeroHabilitacion = calculador.calcular(
                base, List.of(habilitacion, bloqueo));
        List<IntervaloHorario> resultadoPrimeroBloqueo = calculador.calcular(
                base, List.of(bloqueo, habilitacion));

        assertThat(resultadoPrimeroHabilitacion).containsExactly(
                intervalo("08:00", "14:00"));
        assertThat(resultadoPrimeroBloqueo).isEqualTo(resultadoPrimeroHabilitacion);
    }

    @Test
    void cierreCompletoDominaTodasLasDemasExcepciones() {
        List<ExcepcionAgenda> excepciones = List.of(
                excepcion(TipoExcepcion.HABILITACION_EXTRAORDINARIA, "18:00", "21:00"),
                excepcion(TipoExcepcion.DIA_NO_LABORABLE, null, null),
                excepcion(TipoExcepcion.MODIFICACION_HORARIO, "10:00", "15:00"));

        assertThat(calculador.calcular(List.of(intervalo("08:00", "12:00")), excepciones))
                .isEmpty();
    }

    @Test
    void excepcionHorariaLegacySinHorasConservaSemanticaDeCierreCompleto() {
        ExcepcionAgenda legacy = excepcion(TipoExcepcion.EXCEPCION_HORARIA, null, null);

        assertThat(calculador.calcular(
                List.of(intervalo("08:00", "12:00")), List.of(legacy)))
                .isEmpty();
    }

    @Test
    void excepcionInactivaNoAfectaLaDisponibilidad() {
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, "09:00", "10:00");
        bloqueo.setActiva(false);
        List<IntervaloHorario> base = List.of(intervalo("08:00", "12:00"));

        assertThat(calculador.calcular(base, List.of(bloqueo))).isEqualTo(base);
    }

    @Test
    void excepcionHorariaModernaSinRangoNoSeIgnoraSilenciosamente() {
        ExcepcionAgenda bloqueoInvalido = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, null, null);

        assertThatThrownBy(() -> calculador.calcular(
                List.of(intervalo("08:00", "12:00")), List.of(bloqueoInvalido)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requiere hora de inicio y hora de fin");
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
