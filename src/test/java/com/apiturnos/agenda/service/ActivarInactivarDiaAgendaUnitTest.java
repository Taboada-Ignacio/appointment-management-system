package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivarInactivarDiaAgendaUnitTest {

    @Mock
    private DiaAgendaRepository diaAgendaRepository;

    @Mock
    private GestorCambioEstado gestorCambioEstado;

    @Mock
    private RegistradorAuditoria registradorAuditoria;

    @InjectMocks
    private ActivarInactivarDiaAgenda activarInactivarDiaAgenda;

    private DiaAgenda dia;

    @BeforeEach
    void setUp() {
        Profesional profesional = new Profesional();
        profesional.setId(1L);

        AgendaAnual agendaAnual = new AgendaAnual();
        agendaAnual.setProfesional(profesional);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agendaAnual);

        dia = new DiaAgenda();
        dia.setId(10L);
        dia.setMesAgenda(mes);
    }

    @Test
    @DisplayName("activar - Registra estado ACTIVO inicial si no tenía estado previo")
    void testActivarInicial() {
        when(diaAgendaRepository.findById(10L)).thenReturn(Optional.of(dia));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 10L)).thenReturn(null);

        activarInactivarDiaAgenda.activar(1L, 10L, "admin");

        verify(gestorCambioEstado).registrarCambioInicial(eq(AmbitoEstado.DIA_AGENDA), eq(10L), eq("ACTIVO"), eq("admin"), any());
        verify(registradorAuditoria).registrar(eq("AGENDA"), eq("DiaAgenda"), eq(10L), any(), eq("admin"), eq(1L), any());
    }

    @Test
    @DisplayName("activar - Cambia estado a ACTIVO si estaba INACTIVO")
    void testActivarDesdeInactivo() {
        when(diaAgendaRepository.findById(10L)).thenReturn(Optional.of(dia));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 10L)).thenReturn("INACTIVO");

        activarInactivarDiaAgenda.activar(1L, 10L, "admin");

        verify(gestorCambioEstado).registrarCambio(eq(AmbitoEstado.DIA_AGENDA), eq(10L), eq("ACTIVO"), eq("admin"), any(), isNull());
    }

    @Test
    @DisplayName("activar - Lanza excepción si el día no pertenece al profesional")
    void testActivarNoPertenece() {
        when(diaAgendaRepository.findById(10L)).thenReturn(Optional.of(dia));

        assertThatThrownBy(() -> activarInactivarDiaAgenda.activar(99L, 10L, "admin"))
                .isInstanceOf(ClienteNoPerteneceProfesionalException.class);
    }

    @Test
    @DisplayName("activar - Lanza excepción si el día no existe")
    void testActivarNoExiste() {
        when(diaAgendaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activarInactivarDiaAgenda.activar(1L, 999L, "admin"))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    @DisplayName("inactivar - Cambia estado a INACTIVO si estaba ACTIVO")
    void testInactivar() {
        when(diaAgendaRepository.findById(10L)).thenReturn(Optional.of(dia));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, 10L)).thenReturn("ACTIVO");

        activarInactivarDiaAgenda.inactivar(1L, 10L, "admin");

        verify(gestorCambioEstado).registrarCambio(eq(AmbitoEstado.DIA_AGENDA), eq(10L), eq("INACTIVO"), eq("admin"), any(), isNull());
    }
}

