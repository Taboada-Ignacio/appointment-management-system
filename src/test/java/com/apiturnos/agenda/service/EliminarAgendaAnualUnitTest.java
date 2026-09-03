package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminarAgendaAnualUnitTest {

    @Mock
    private AgendaAnualRepository agendaAnualRepository;

    @Mock
    private MesAgendaRepository mesAgendaRepository;

    @Mock
    private DiaAgendaRepository diaAgendaRepository;

    @Mock
    private BrechaHorariaRepository brechaHorariaRepository;

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private TurnoHistorialRepository turnoHistorialRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private CambioEstadoRepository cambioEstadoRepository;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private RegistradorAuditoria registradorAuditoria;

    @Mock
    private EntityManager entityManager;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private EliminarAgendaAnual eliminarAgendaAnual;

    private Profesional profesional;
    private AgendaAnual agenda;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Dr. Favaloro");

        agenda = new AgendaAnual();
        agenda.setId(10L);
        agenda.setProfesional(profesional);
        agenda.setAnio(2026);
    }

    @Test
    @DisplayName("Elimina en cascada la agenda del año actual con meses, días y turnos")
    void eliminar_anioActual_exitoso() {
        when(profesionalRepository.existsById(1L)).thenReturn(true);
        when(agendaAnualRepository.findByProfesionalIdAndAnio(1L, 2026)).thenReturn(Optional.of(agenda));
        when(mesAgendaRepository.findIdsByAgendaAnualId(10L)).thenReturn(List.of(100L));
        when(diaAgendaRepository.findIdsByMesAgendaIdIn(List.of(100L))).thenReturn(List.of(200L));
        when(turnoRepository.findIdsByDiaAgendaIdIn(List.of(200L))).thenReturn(List.of(300L));

        eliminarAgendaAnual.ejecutarAnioActual(1L, "admin");

        verify(notificacionRepository).desvincularTurnos(List.of(300L));
        verify(turnoHistorialRepository).deleteByTurnoIdIn(List.of(300L));
        verify(cambioEstadoRepository).deleteByAmbitoAndEntidadIdIn(AmbitoEstado.TURNO, List.of(300L));
        verify(turnoRepository).deleteAllByIdIn(List.of(300L));

        verify(turnoHistorialRepository).desvincularDiasAnteriores(List.of(200L));
        verify(brechaHorariaRepository).deleteByDiaAgendaIdIn(List.of(200L));
        verify(cambioEstadoRepository).deleteByAmbitoAndEntidadIdIn(AmbitoEstado.DIA_AGENDA, List.of(200L));
        verify(diaAgendaRepository).deleteAllByIdIn(List.of(200L));

        verify(cambioEstadoRepository).deleteByAmbitoAndEntidadIdIn(AmbitoEstado.MES_AGENDA, List.of(100L));
        verify(mesAgendaRepository).deleteByAgendaAnualId(10L);

        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(agendaAnualRepository).deleteByIdDirecto(10L);
        verify(registradorAuditoria).registrar(
                eq("AGENDA"), eq("AgendaAnual"), eq(10L),
                eq(OperacionAuditoria.DELETE), eq("admin"), eq(1L),
                eq("Eliminación en cascada de agenda anual del año 2026"));
    }

    @Test
    @DisplayName("Elimina agenda sin meses asociados")
    void eliminar_agendaSinMeses_exitoso() {
        when(profesionalRepository.existsById(1L)).thenReturn(true);
        when(agendaAnualRepository.findByProfesionalIdAndAnio(1L, 2026)).thenReturn(Optional.of(agenda));
        when(mesAgendaRepository.findIdsByAgendaAnualId(10L)).thenReturn(Collections.emptyList());

        eliminarAgendaAnual.ejecutar(1L, 2026, "admin");

        verify(entityManager).flush();
        verify(entityManager).clear();
        verify(agendaAnualRepository).deleteByIdDirecto(10L);
        verify(registradorAuditoria).registrar(
                eq("AGENDA"), eq("AgendaAnual"), eq(10L),
                eq(OperacionAuditoria.DELETE), eq("admin"), eq(1L),
                eq("Eliminación en cascada de agenda anual del año 2026"));
    }

    @Test
    @DisplayName("Lanza NegocioException si profesionalId es nulo")
    void eliminar_profesionalIdNulo_lanzaNegocioException() {
        assertThrows(NegocioException.class, () ->
                eliminarAgendaAnual.ejecutar(null, 2026, "admin"));
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si profesional no existe")
    void eliminar_profesionalNoExiste_lanzaEntidadNoEncontradaException() {
        when(profesionalRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntidadNoEncontradaException.class, () ->
                eliminarAgendaAnual.ejecutar(99L, 2026, "admin"));
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si agenda no existe")
    void eliminar_agendaNoExiste_lanzaEntidadNoEncontradaException() {
        when(profesionalRepository.existsById(1L)).thenReturn(true);
        when(agendaAnualRepository.findByProfesionalIdAndAnio(1L, 2026)).thenReturn(Optional.empty());

        assertThrows(EntidadNoEncontradaException.class, () ->
                eliminarAgendaAnual.ejecutar(1L, 2026, "admin"));
    }
}

