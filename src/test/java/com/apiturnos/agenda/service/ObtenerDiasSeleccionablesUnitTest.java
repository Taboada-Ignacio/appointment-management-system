package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.DiaSeleccionableResponseDto;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.service.EvaluadorDisponibilidadTurnoManual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerDiasSeleccionablesUnitTest {

    private static final Long PROFESIONAL_ID = 1L;
    private static final LocalDate HOY = LocalDate.of(2026, 9, 10);
    private static final Instant AHORA = HOY.atTime(12, 0).toInstant(ZoneOffset.UTC);

    @Mock private DiaAgendaRepository diaAgendaRepository;
    @Mock private ExcepcionAgendaRepository excepcionAgendaRepository;
    @Mock private GestorCambioEstado gestorCambioEstado;
    @Mock private EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad;
    @Mock private ProfesionalRepository profesionalRepository;

    private ObtenerDiasSeleccionables servicio;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
        servicio = new ObtenerDiasSeleccionables(
                diaAgendaRepository,
                excepcionAgendaRepository,
                gestorCambioEstado,
                evaluadorDisponibilidad,
                profesionalRepository,
                clock);

        lenient().when(profesionalRepository.existsById(PROFESIONAL_ID)).thenReturn(true);
        lenient().when(excepcionAgendaRepository.findActivasAplicablesAFecha(eq(PROFESIONAL_ID), any()))
                .thenReturn(List.of());
        lenient().when(evaluadorDisponibilidad.hayCierreCompleto(anyCollection())).thenReturn(false);
    }

    @Test
    @DisplayName("Profesional inexistente lanza EntidadNoEncontradaException")
    void testProfesionalInexistente() {
        when(profesionalRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.ejecutar(99L, HOY, HOY.plusDays(5)))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    @DisplayName("Rango de fechas inválido lanza NegocioException")
    void testRangoFechasInvalido() {
        assertThatThrownBy(() -> servicio.ejecutar(PROFESIONAL_ID, HOY.plusDays(5), HOY))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    @DisplayName("Clasifica correctamente días ACTIVO, EN_TRANSCURSO, INACTIVO, FINALIZADO y con excepción")
    void testClasificacionDias() {
        DiaAgenda diaActivo = dia(10L, HOY.plusDays(1));
        DiaAgenda diaEnTranscurso = dia(11L, HOY);
        DiaAgenda diaInactivo = dia(12L, HOY.plusDays(2));
        DiaAgenda diaFinalizado = dia(13L, HOY.plusDays(3));
        DiaAgenda diaConCierre = dia(14L, HOY.plusDays(4));

        when(diaAgendaRepository.findByProfesionalIdAndFechaBetween(PROFESIONAL_ID, HOY, HOY.plusDays(10)))
                .thenReturn(List.of(diaActivo, diaEnTranscurso, diaInactivo, diaFinalizado, diaConCierre));

        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 10L)).thenReturn("ACTIVO");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 11L)).thenReturn("EN_TRANSCURSO");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 12L)).thenReturn("INACTIVO");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 13L)).thenReturn("FINALIZADO");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 14L)).thenReturn("ACTIVO");

        ExcepcionAgenda cierre = new ExcepcionAgenda();
        cierre.setTipo(TipoExcepcion.VACACIONES);
        when(excepcionAgendaRepository.findActivasAplicablesAFecha(PROFESIONAL_ID, HOY.plusDays(4)))
                .thenReturn(List.of(cierre));
        when(evaluadorDisponibilidad.hayCierreCompleto(List.of(cierre))).thenReturn(true);

        List<DiaSeleccionableResponseDto> resultado = servicio.ejecutar(PROFESIONAL_ID, HOY, HOY.plusDays(10));

        assertThat(resultado).hasSize(5);

        // EN_TRANSCURSO (hoy)
        assertThat(resultado.get(0).getDiaAgendaId()).isEqualTo(11L);
        assertThat(resultado.get(0).isSeleccionable()).isTrue();
        assertThat(resultado.get(0).getMensaje()).contains("futuros");

        // ACTIVO (+1 día)
        assertThat(resultado.get(1).getDiaAgendaId()).isEqualTo(10L);
        assertThat(resultado.get(1).isSeleccionable()).isTrue();
        assertThat(resultado.get(1).getMensaje()).isNull();

        // INACTIVO (+2 días)
        assertThat(resultado.get(2).getDiaAgendaId()).isEqualTo(12L);
        assertThat(resultado.get(2).isSeleccionable()).isFalse();
        assertThat(resultado.get(2).getMensaje()).isEqualTo("Día inactivo");

        // FINALIZADO (+3 días)
        assertThat(resultado.get(3).getDiaAgendaId()).isEqualTo(13L);
        assertThat(resultado.get(3).isSeleccionable()).isFalse();
        assertThat(resultado.get(3).getMensaje()).isEqualTo("Día finalizado");

        // Cerrado por excepción (+4 días)
        assertThat(resultado.get(4).getDiaAgendaId()).isEqualTo(14L);
        assertThat(resultado.get(4).isSeleccionable()).isFalse();
        assertThat(resultado.get(4).getMensaje()).contains("excepción");
    }

    private DiaAgenda dia(Long id, LocalDate fecha) {
        DiaAgenda dia = new DiaAgenda();
        dia.setId(id);
        dia.setFecha(fecha);
        return dia;
    }
}

