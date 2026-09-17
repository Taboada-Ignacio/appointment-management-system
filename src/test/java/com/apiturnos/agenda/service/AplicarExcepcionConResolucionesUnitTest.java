package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.DecisionTurnoAfectadoRequestDto;
import com.apiturnos.agenda.dto.TipoDecisionTurnoAfectado;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.AfectacionTurnoExcepcionRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.service.ReprogramarTurno;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AplicarExcepcionConResolucionesUnitTest {

    @Test
    void rechazaReprogramarEnUnDiaDeLaExcepcionNueva() {
        DiaAgendaRepository dias = mock(DiaAgendaRepository.class);
        AfectacionTurnoExcepcionRepository afectaciones = mock(AfectacionTurnoExcepcionRepository.class);
        ReprogramarTurno reprogramar = mock(ReprogramarTurno.class);
        AplicarExcepcionConResoluciones servicio = new AplicarExcepcionConResoluciones(
                mock(ProfesionalRepository.class), mock(ExcepcionAgendaRepository.class), afectaciones, dias,
                mock(ValidadorExcepcionAgenda.class), mock(EvaluarImpactoExcepcionAgenda.class),
                mock(TokenImpactoExcepcionAgenda.class), mock(GestorCambioEstado.class),
                mock(ProcesarBajasTurnosPorExcepcion.class), reprogramar,
                mock(RegistradorAuditoria.class), mock(DetectorCoincidenciasExcepcionAgenda.class),
                mock(SincronizarEstadoDiasPorExcepcion.class));

        Profesional profesional = new Profesional();
        profesional.setId(1L);
        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setProfesional(profesional);
        excepcion.setTipo(TipoExcepcion.VACACIONES);
        excepcion.setFechaInicio(LocalDate.of(2026, 9, 20));
        excepcion.setFechaFin(LocalDate.of(2026, 9, 22));

        DiaAgenda destino = new DiaAgenda();
        destino.setId(9L);
        destino.setFecha(LocalDate.of(2026, 9, 21));
        when(dias.findByIdAndProfesionalId(9L, 1L)).thenReturn(Optional.of(destino));

        Turno turno = new Turno();
        turno.setId(7L);
        DecisionTurnoAfectadoRequestDto decision = new DecisionTurnoAfectadoRequestDto(
                7L, TipoDecisionTurnoAfectado.REPROGRAMAR, 9L,
                Instant.parse("2026-09-21T12:00:00Z"), Instant.parse("2026-09-21T12:30:00Z"), null);

        assertThatThrownBy(() -> servicio.aplicarResoluciones(excepcion, List.of(turno), List.of(decision), "test"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("día afectado");

        verifyNoInteractions(afectaciones, reprogramar);
    }
}
