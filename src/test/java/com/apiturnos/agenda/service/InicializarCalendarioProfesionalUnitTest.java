package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.BrechaHorariaRequestDto;
import com.apiturnos.agenda.dto.DiaSemanaConfiguracionDto;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializarCalendarioProfesionalUnitTest {

    @Mock AgendaAnualRepository agendaAnualRepository;
    @Mock MesAgendaRepository mesAgendaRepository;
    @Mock DiaAgendaRepository diaAgendaRepository;
    @Mock CrearAgendaAnual crearAgendaAnual;
    @Mock ConfigurarMesModoSemana configurarMesModoSemana;
    @Mock ActivarInactivarMesAgenda activarInactivarMesAgenda;
    @Mock GestorCambioEstado gestorCambioEstado;

    @Test
    void diciembreCreaAgendaSiguienteYConfiguraEnero() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-15T12:00:00Z"), ZoneOffset.UTC);
        AgendaAnual agenda2026 = agenda(10L, 2026);
        AgendaAnual agenda2027 = agenda(11L, 2027);
        MesAgenda diciembre = mes(120L, 12, agenda2026);
        MesAgenda enero = mes(121L, 1, agenda2027);

        when(agendaAnualRepository.findByProfesionalIdAndAnio(1L, 2026))
                .thenReturn(Optional.empty(), Optional.of(agenda2026));
        when(agendaAnualRepository.findByProfesionalIdAndAnio(1L, 2027))
                .thenReturn(Optional.empty(), Optional.of(agenda2027));
        when(crearAgendaAnual.ejecutar(eq(1L), eq(2026), any())).thenReturn(agenda2026);
        when(crearAgendaAnual.ejecutar(eq(1L), eq(2027), any())).thenReturn(agenda2027);
        when(mesAgendaRepository.findByAgendaAnualIdAndNroMes(10L, 12)).thenReturn(Optional.of(diciembre));
        when(mesAgendaRepository.findByAgendaAnualIdAndNroMes(11L, 1)).thenReturn(Optional.of(enero));
        when(gestorCambioEstado.obtenerNombreEstadoActual(eq(AmbitoEstado.MES_AGENDA), anyLong()))
                .thenReturn("ACTIVO");
        when(diaAgendaRepository.findByMesAgendaId(anyLong())).thenReturn(List.of());
        when(gestorCambioEstado.obtenerEstadosActualesPorEntidades(eq(AmbitoEstado.DIA_AGENDA), any()))
                .thenReturn(Map.of());

        var servicio = new InicializarCalendarioProfesional(
                agendaAnualRepository, mesAgendaRepository, diaAgendaRepository, crearAgendaAnual,
                configurarMesModoSemana, activarInactivarMesAgenda, gestorCambioEstado, clock);
        var dia = new DiaSemanaConfiguracionDto(DayOfWeek.MONDAY,
                List.of(new BrechaHorariaRequestDto(LocalTime.of(9, 0), LocalTime.of(13, 0))));

        var respuesta = servicio.ejecutar(1L, List.of(dia), true, "admin");

        assertThat(respuesta.agendasAnuales()).containsExactly(2026, 2027);
        assertThat(respuesta.mesesConfigurados()).extracting(mes -> mes.nroMes())
                .containsExactly(12, 1);
        verify(crearAgendaAnual).ejecutar(1L, 2026, "admin");
        verify(crearAgendaAnual).ejecutar(1L, 2027, "admin");
        verify(configurarMesModoSemana).ejecutar(eq(1L), eq(120L), any(), eq("admin"));
        verify(configurarMesModoSemana).ejecutar(eq(1L), eq(121L), any(), eq("admin"));
    }

    private AgendaAnual agenda(Long id, int anio) {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setId(id);
        agenda.setAnio(anio);
        return agenda;
    }

    private MesAgenda mes(Long id, int nroMes, AgendaAnual agenda) {
        MesAgenda mes = new MesAgenda();
        mes.setId(id);
        mes.setNroMes(nroMes);
        mes.setAgendaAnual(agenda);
        return mes;
    }
}
