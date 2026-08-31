package com.apiturnos.disponibilidad.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalcularDisponibilidadDiaUnitTest {

    private static final Long PROFESIONAL_ID = 7L;
    private static final Long DIA_ID = 42L;
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 15);

    @Mock
    private DiaAgendaRepository diaAgendaRepository;

    @Mock
    private BrechaHorariaRepository brechaHorariaRepository;

    @Mock
    private ExcepcionAgendaRepository excepcionAgendaRepository;

    @Mock
    private GestorCambioEstado gestorCambioEstado;

    private CalcularDisponibilidadDia casoDeUso;

    @BeforeEach
    void setUp() {
        casoDeUso = new CalcularDisponibilidadDia(
                diaAgendaRepository,
                brechaHorariaRepository,
                excepcionAgendaRepository,
                gestorCambioEstado,
                new CalculadorDisponibilidadEfectiva());
    }

    @Test
    void habilitaUnDiaBaseInactivoSinModificarNiPersistirSusBrechas() {
        DiaAgenda dia = new DiaAgenda();
        dia.setId(DIA_ID);
        dia.setFecha(FECHA);

        BrechaHoraria brechaBase = new BrechaHoraria();
        brechaBase.setId(10L);
        brechaBase.setDiaAgenda(dia);
        brechaBase.setHoraInicioAtencion(LocalTime.of(8, 0));
        brechaBase.setHoraFinAtencion(LocalTime.of(12, 0));

        ExcepcionAgenda habilitacion = new ExcepcionAgenda();
        habilitacion.setTipo(TipoExcepcion.HABILITACION_EXTRAORDINARIA);
        habilitacion.setFechaInicio(FECHA);
        habilitacion.setFechaFin(FECHA);
        habilitacion.setHoraInicio(LocalTime.of(18, 0));
        habilitacion.setHoraFin(LocalTime.of(21, 0));
        habilitacion.setActiva(true);

        when(diaAgendaRepository.findByProfesionalIdAndFecha(PROFESIONAL_ID, FECHA))
                .thenReturn(Optional.of(dia));
        when(brechaHorariaRepository.findByDiaAgendaId(DIA_ID))
                .thenReturn(List.of(brechaBase));
        when(excepcionAgendaRepository.findActivasAplicablesAFecha(PROFESIONAL_ID, FECHA))
                .thenReturn(List.of(habilitacion));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, DIA_ID))
                .thenReturn("INACTIVO");

        List<IntervaloHorario> resultado = casoDeUso.ejecutar(PROFESIONAL_ID, FECHA);

        assertThat(resultado).containsExactly(
                new IntervaloHorario(LocalTime.of(18, 0), LocalTime.of(21, 0)));
        assertThat(brechaBase.getHoraInicioAtencion()).isEqualTo(LocalTime.of(8, 0));
        assertThat(brechaBase.getHoraFinAtencion()).isEqualTo(LocalTime.of(12, 0));
        assertThat(dia.getFecha()).isEqualTo(FECHA);

        verify(diaAgendaRepository, never()).save(any(DiaAgenda.class));
        verify(brechaHorariaRepository, never()).save(any(BrechaHoraria.class));
        verify(excepcionAgendaRepository, never()).save(any(ExcepcionAgenda.class));
    }

    @Test
    void diaFinalizadoRetornaDisponibilidadVaciaYSinReapertura() {
        DiaAgenda dia = new DiaAgenda();
        dia.setId(DIA_ID);
        dia.setFecha(FECHA);

        BrechaHoraria brechaBase = new BrechaHoraria();
        brechaBase.setId(10L);
        brechaBase.setDiaAgenda(dia);
        brechaBase.setHoraInicioAtencion(LocalTime.of(8, 0));
        brechaBase.setHoraFinAtencion(LocalTime.of(12, 0));

        ExcepcionAgenda habilitacion = new ExcepcionAgenda();
        habilitacion.setTipo(TipoExcepcion.HABILITACION_EXTRAORDINARIA);
        habilitacion.setFechaInicio(FECHA);
        habilitacion.setFechaFin(FECHA);
        habilitacion.setHoraInicio(LocalTime.of(18, 0));
        habilitacion.setHoraFin(LocalTime.of(21, 0));
        habilitacion.setActiva(true);

        when(diaAgendaRepository.findByProfesionalIdAndFecha(PROFESIONAL_ID, FECHA))
                .thenReturn(Optional.of(dia));
        when(brechaHorariaRepository.findByDiaAgendaId(DIA_ID))
                .thenReturn(List.of(brechaBase));
        when(excepcionAgendaRepository.findActivasAplicablesAFecha(PROFESIONAL_ID, FECHA))
                .thenReturn(List.of(habilitacion));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, DIA_ID))
                .thenReturn("FINALIZADO");

        List<IntervaloHorario> resultado = casoDeUso.ejecutar(PROFESIONAL_ID, FECHA);

        assertThat(resultado).isEmpty();
    }

    @Test
    void diaSinCambioEstadoHistoricoUsaBrechasBasePorCompatibilidad() {
        DiaAgenda dia = new DiaAgenda();
        dia.setId(DIA_ID);
        dia.setFecha(FECHA);

        BrechaHoraria brechaBase = new BrechaHoraria();
        brechaBase.setId(10L);
        brechaBase.setDiaAgenda(dia);
        brechaBase.setHoraInicioAtencion(LocalTime.of(8, 0));
        brechaBase.setHoraFinAtencion(LocalTime.of(12, 0));

        when(diaAgendaRepository.findByProfesionalIdAndFecha(PROFESIONAL_ID, FECHA))
                .thenReturn(Optional.of(dia));
        when(brechaHorariaRepository.findByDiaAgendaId(DIA_ID))
                .thenReturn(List.of(brechaBase));
        when(excepcionAgendaRepository.findActivasAplicablesAFecha(PROFESIONAL_ID, FECHA))
                .thenReturn(List.of());
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, DIA_ID))
                .thenReturn(null);

        List<IntervaloHorario> resultado = casoDeUso.ejecutar(PROFESIONAL_ID, FECHA);

        assertThat(resultado).containsExactly(
                new IntervaloHorario(LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }
}
