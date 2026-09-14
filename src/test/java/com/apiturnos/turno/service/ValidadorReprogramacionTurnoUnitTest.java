package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.turno.model.Turno;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidadorReprogramacionTurnoUnitTest {

    @Test
    void rechazaHorarioPasadoDelDiaActualAunqueElDiaSigaActivo() {
        GestorCambioEstado estados = mock(GestorCambioEstado.class);
        ValidadorReprogramacionTurno validador = new ValidadorReprogramacionTurno(
                mock(BrechaHorariaRepository.class),
                mock(ExcepcionAgendaRepository.class),
                estados,
                mock(EvaluadorDisponibilidadTurnoManual.class),
                mock(VerificarCapacidadTipoAtencion.class),
                mock(VerificadorCapacidad.class),
                mock(ConfiguracionRepository.class),
                Clock.fixed(Instant.parse("2026-09-13T19:30:00Z"), ZoneOffset.UTC));

        Profesional profesional = new Profesional();
        profesional.setId(1L);
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesional);
        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        DiaAgenda dia = new DiaAgenda();
        dia.setId(13L);
        dia.setFecha(LocalDate.of(2026, 9, 13));
        dia.setMesAgenda(mes);
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setProfesional(profesional);
        Turno turno = new Turno();
        turno.setId(3L);
        turno.setDiaAgenda(dia);
        turno.setCliente(cliente);

        when(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, 2L)).thenReturn("HABILITADO");
        when(estados.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 13L)).thenReturn("ACTIVO");

        assertThatThrownBy(() -> validador.validar(
                turno,
                dia,
                Instant.parse("2026-09-13T12:00:00Z"),
                Instant.parse("2026-09-13T12:30:00Z")))
                .isInstanceOf(ReprogramacionTurnoInvalidaException.class)
                .hasMessageContaining("ya comenzo");
    }
}
