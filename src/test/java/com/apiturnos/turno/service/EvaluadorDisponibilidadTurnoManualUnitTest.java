package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.disponibilidad.service.CalculadorDisponibilidadEfectiva;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.MotivoRechazoTurnoManual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluadorDisponibilidadTurnoManualUnitTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 10);

    private EvaluadorDisponibilidadTurnoManual evaluador;
    private DiaAgenda dia;
    private BrechaHoraria brechaBase;

    @BeforeEach
    void setUp() {
        CalcularDisponibilidadDia disponibilidad = new CalcularDisponibilidadDia(
                null, null, null, null, new CalculadorDisponibilidadEfectiva());
        evaluador = new EvaluadorDisponibilidadTurnoManual(disponibilidad);

        dia = new DiaAgenda();
        dia.setId(1L);
        dia.setFecha(FECHA);

        brechaBase = new BrechaHoraria();
        brechaBase.setDiaAgenda(dia);
        brechaBase.setHoraInicioAtencion(LocalTime.of(8, 0));
        brechaBase.setHoraFinAtencion(LocalTime.of(12, 0));
    }

    @Test
    void dentroDeBrechaNoGeneraAdvertencia() {
        List<AdvertenciaTurnoManual> resultado = evaluador.evaluar(
                dia, "ACTIVO", intervalo(9, 0, 9, 30), List.of(brechaBase), List.of());

        assertThat(resultado).isEmpty();
    }

    @Test
    void fueraDeBrechaGeneraAdvertenciaYNoExcepcion() {
        List<AdvertenciaTurnoManual> resultado = evaluador.evaluar(
                dia, "ACTIVO", intervalo(14, 0, 14, 30), List.of(brechaBase), List.of());

        assertThat(resultado).containsExactly(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA);
    }

    @Test
    void vacacionesBloqueanElAlta() {
        ExcepcionAgenda vacaciones = excepcion(TipoExcepcion.VACACIONES, null, null);

        assertThatThrownBy(() -> evaluador.evaluar(
                dia, "ACTIVO", intervalo(9, 0, 9, 30), List.of(brechaBase), List.of(vacaciones)))
                .isInstanceOf(TurnoManualNoPermitidoException.class)
                .extracting(ex -> ((TurnoManualNoPermitidoException) ex).getMotivo())
                .isEqualTo(MotivoRechazoTurnoManual.DIA_CERRADO_POR_EXCEPCION);
    }

    @Test
    void bloqueoHorarioImpideCualquierInterseccion() {
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThatThrownBy(() -> evaluador.evaluar(
                dia, "ACTIVO", intervalo(9, 45, 10, 15), List.of(brechaBase), List.of(bloqueo)))
                .isInstanceOf(TurnoManualNoPermitidoException.class)
                .extracting(ex -> ((TurnoManualNoPermitidoException) ex).getMotivo())
                .isEqualTo(MotivoRechazoTurnoManual.HORARIO_BLOQUEADO_POR_EXCEPCION);
    }

    @Test
    void habilitacionExtraordinariaPermiteHorarioFueraDeLaBaseSinAdvertencia() {
        ExcepcionAgenda habilitacion = excepcion(
                TipoExcepcion.HABILITACION_EXTRAORDINARIA,
                LocalTime.of(14, 0), LocalTime.of(16, 0));

        List<AdvertenciaTurnoManual> resultado = evaluador.evaluar(
                dia, "ACTIVO", intervalo(14, 0, 14, 30),
                List.of(brechaBase), List.of(habilitacion));

        assertThat(resultado).isEmpty();
    }

    @Test
    void modificacionHorarioReemplazaLaDisponibilidadParaLaValidacion() {
        ExcepcionAgenda modificacion = excepcion(
                TipoExcepcion.MODIFICACION_HORARIO,
                LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertThat(evaluador.evaluar(
                dia, "ACTIVO", intervalo(14, 0, 14, 30),
                List.of(brechaBase), List.of(modificacion)))
                .isEmpty();
        assertThat(evaluador.evaluar(
                dia, "ACTIVO", intervalo(9, 0, 9, 30),
                List.of(brechaBase), List.of(modificacion)))
                .containsExactly(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA);
    }

    @Test
    void habilitacionNoPuedeIgnorarUnBloqueoExplicitoSuperpuesto() {
        ExcepcionAgenda bloqueo = excepcion(
                TipoExcepcion.BLOQUEO_HORARIO, LocalTime.of(14, 0), LocalTime.of(15, 0));
        ExcepcionAgenda habilitacion = excepcion(
                TipoExcepcion.HABILITACION_EXTRAORDINARIA, LocalTime.of(14, 0), LocalTime.of(16, 0));

        assertThatThrownBy(() -> evaluador.evaluar(
                dia, "ACTIVO", intervalo(14, 15, 14, 45),
                List.of(brechaBase), List.of(bloqueo, habilitacion)))
                .isInstanceOf(TurnoManualNoPermitidoException.class);
    }

    private ExcepcionAgenda excepcion(TipoExcepcion tipo, LocalTime inicio, LocalTime fin) {
        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setTipo(tipo);
        excepcion.setFechaInicio(FECHA);
        excepcion.setFechaFin(FECHA);
        excepcion.setActiva(true);
        excepcion.setHoraInicio(inicio);
        excepcion.setHoraFin(fin);
        return excepcion;
    }

    private IntervaloHorario intervalo(int horaInicio, int minutoInicio, int horaFin, int minutoFin) {
        return new IntervaloHorario(
                LocalTime.of(horaInicio, minutoInicio),
                LocalTime.of(horaFin, minutoFin));
    }
}
