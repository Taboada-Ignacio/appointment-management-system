package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.AfectacionTurnoExcepcion;
import com.apiturnos.agenda.model.EstadoResolucionAfectacion;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.AfectacionTurnoExcepcionRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.turno.model.Turno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CancelarExcepcionAgendaUnitTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void cancelarRestauraSoloTurnosPendientes() {
        var excepciones = mock(ExcepcionAgendaRepository.class);
        var afectaciones = mock(AfectacionTurnoExcepcionRepository.class);
        var estados = mock(GestorCambioEstado.class);
        var auditoria = mock(RegistradorAuditoria.class);
        var dias = mock(SincronizarEstadoDiasPorExcepcion.class);
        var motivos = mock(com.apiturnos.turno.repository.MotivoBajaTurnoRepository.class);
        var cancelar = new CancelarExcepcionAgenda(excepciones, auditoria, dias, afectaciones, estados, motivos, CLOCK);

        var excepcion = new ExcepcionAgenda();
        excepcion.setId(5L);
        excepcion.setFechaInicio(LocalDate.of(2026, 9, 15));
        excepcion.setFechaFin(LocalDate.of(2026, 9, 23));
        excepcion.setActiva(true);
        when(excepciones.findByIdAndProfesionalId(5L, 1L)).thenReturn(Optional.of(excepcion));
        when(excepciones.save(excepcion)).thenReturn(excepcion);

        var turno = mock(Turno.class);
        when(turno.getId()).thenReturn(42L);
        var pendiente = new AfectacionTurnoExcepcion();
        pendiente.setTurno(turno);
        pendiente.setEstadoTurnoAnterior("ASIGNADO");
        pendiente.setEstadoResolucion(EstadoResolucionAfectacion.PENDIENTE);
        var resuelto = new AfectacionTurnoExcepcion();
        resuelto.setEstadoResolucion(EstadoResolucionAfectacion.DADO_DE_BAJA);
        when(afectaciones.findByExcepcionAgendaIdOrderByIdAsc(5L)).thenReturn(List.of(pendiente, resuelto));
        when(estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 42L))
                .thenReturn("AFECTADO_POR_EXCEPCION");

        cancelar.ejecutar(1L, 5L, "test");

        verify(estados).registrarCambio(eq(AmbitoEstado.TURNO), eq(42L), eq("ASIGNADO"),
                eq("test"), anyString(), isNull());
        assertThat(pendiente.getEstadoResolucion()).isEqualTo(EstadoResolucionAfectacion.RESTAURADO);
        assertThat(pendiente.getResueltoEn()).isNotNull();
        assertThat(resuelto.getEstadoResolucion()).isEqualTo(EstadoResolucionAfectacion.DADO_DE_BAJA);
        verify(afectaciones, times(1)).save(any());
    }

    @Test
    void eliminaFisicamenteUnaExcepcionFutura() {
        var excepciones = mock(ExcepcionAgendaRepository.class);
        var afectaciones = mock(AfectacionTurnoExcepcionRepository.class);
        var motivos = mock(com.apiturnos.turno.repository.MotivoBajaTurnoRepository.class);
        var cancelar = new CancelarExcepcionAgenda(excepciones, mock(RegistradorAuditoria.class),
                mock(SincronizarEstadoDiasPorExcepcion.class), afectaciones,
                mock(GestorCambioEstado.class), motivos, CLOCK);
        var excepcion = excepcion(5L, LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 20));
        when(excepciones.findByIdAndProfesionalId(5L, 1L)).thenReturn(Optional.of(excepcion));
        when(afectaciones.findByExcepcionAgendaIdOrderByIdAsc(5L)).thenReturn(List.of());

        cancelar.ejecutar(1L, 5L, "test");

        verify(motivos).desvincularExcepcion(5L);
        verify(afectaciones).deleteByExcepcionAgendaId(5L);
        verify(excepciones).delete(excepcion);
        verify(excepciones, never()).save(any());
    }

    @Test
    void rechazaLaBajaDeUnaExcepcionFinalizada() {
        var excepciones = mock(ExcepcionAgendaRepository.class);
        var afectaciones = mock(AfectacionTurnoExcepcionRepository.class);
        var motivos = mock(com.apiturnos.turno.repository.MotivoBajaTurnoRepository.class);
        var cancelar = new CancelarExcepcionAgenda(excepciones, mock(RegistradorAuditoria.class),
                mock(SincronizarEstadoDiasPorExcepcion.class), afectaciones,
                mock(GestorCambioEstado.class), motivos, CLOCK);
        var excepcion = excepcion(5L, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 14));
        when(excepciones.findByIdAndProfesionalId(5L, 1L)).thenReturn(Optional.of(excepcion));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> cancelar.ejecutar(1L, 5L, "test"))
                .isInstanceOf(com.apiturnos.shared.exception.NegocioException.class)
                .hasMessageContaining("ya finalizó");

        verifyNoInteractions(afectaciones, motivos);
        verify(excepciones, never()).save(any());
        verify(excepciones, never()).delete(any());
    }

    private ExcepcionAgenda excepcion(Long id, LocalDate inicio, LocalDate fin) {
        var excepcion = new ExcepcionAgenda();
        excepcion.setId(id);
        excepcion.setFechaInicio(inicio);
        excepcion.setFechaFin(fin);
        excepcion.setActiva(true);
        return excepcion;
    }
}
