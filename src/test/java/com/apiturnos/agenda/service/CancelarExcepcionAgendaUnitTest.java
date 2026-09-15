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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CancelarExcepcionAgendaUnitTest {
    @Test
    void cancelarRestauraSoloTurnosPendientes() {
        var excepciones = mock(ExcepcionAgendaRepository.class);
        var afectaciones = mock(AfectacionTurnoExcepcionRepository.class);
        var estados = mock(GestorCambioEstado.class);
        var auditoria = mock(RegistradorAuditoria.class);
        var dias = mock(SincronizarEstadoDiasPorExcepcion.class);
        var cancelar = new CancelarExcepcionAgenda(excepciones, auditoria, dias, afectaciones, estados);

        var excepcion = new ExcepcionAgenda();
        excepcion.setId(5L);
        excepcion.setFechaInicio(LocalDate.of(2026, 9, 20));
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
}
