package com.apiturnos.service;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.CapacidadAgotadaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.service.VerificadorCapacidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificadorCapacidadUnitTest {

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private GestorCambioEstado gestorCambioEstado;

    @InjectMocks
    private VerificadorCapacidad verificadorCapacidad;

    @Test
    @DisplayName("Cuenta correctamente turnos solapados en estado activo")
    void testContarTurnosActivosSolapados() {
        Instant base = Instant.now();

        Turno t1 = new Turno();
        t1.setId(1L);
        t1.setInicioEstimado(base);
        t1.setFinEstimado(base.plus(30, ChronoUnit.MINUTES));

        Turno t2 = new Turno();
        t2.setId(2L);
        t2.setInicioEstimado(base.plus(15, ChronoUnit.MINUTES));
        t2.setFinEstimado(base.plus(45, ChronoUnit.MINUTES));

        Turno tCancelado = new Turno();
        tCancelado.setId(3L);
        tCancelado.setInicioEstimado(base);
        tCancelado.setFinEstimado(base.plus(30, ChronoUnit.MINUTES));

        when(turnoRepository.findByDiaAgendaId(10L)).thenReturn(List.of(t1, t2, tCancelado));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 1L)).thenReturn("ASIGNADO");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 2L)).thenReturn("PENDIENTE_DE_APROBACION");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 3L)).thenReturn("CANCELADO");

        int count = verificadorCapacidad.contarTurnosActivosSolapados(
                10L, base.plus(10, ChronoUnit.MINUTES), base.plus(40, ChronoUnit.MINUTES));

        // t1 and t2 overlap with [10, 40] and are active; tCancelado is CANCELADO so ignored
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Autogestión lanza CapacidadAgotadaException cuando solapados >= maxSimultaneos")
    void testAutogestionLanzaCapacidadAgotada() {
        Instant base = Instant.now();

        Turno t1 = new Turno();
        t1.setId(1L);
        t1.setInicioEstimado(base);
        t1.setFinEstimado(base.plus(30, ChronoUnit.MINUTES));

        when(turnoRepository.findByDiaAgendaId(10L)).thenReturn(List.of(t1));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 1L)).thenReturn("ASIGNADO");

        assertThatThrownBy(() -> verificadorCapacidad.verificarCapacidadAutoGestion(
                10L, base, base.plus(30, ChronoUnit.MINUTES), 1))
                .isInstanceOf(CapacidadAgotadaException.class);
    }

    @Test
    @DisplayName("Verificación manual devuelve true si se excede capacidad pero no lanza excepción")
    void testExcedidaCapacidadManual() {
        Instant base = Instant.now();

        Turno t1 = new Turno();
        t1.setId(1L);
        t1.setInicioEstimado(base);
        t1.setFinEstimado(base.plus(30, ChronoUnit.MINUTES));

        when(turnoRepository.findByDiaAgendaId(10L)).thenReturn(List.of(t1));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 1L)).thenReturn("ASIGNADO");

        boolean excedida = verificadorCapacidad.excedidaCapacidadManual(
                10L, base, base.plus(30, ChronoUnit.MINUTES), 1);

        assertThat(excedida).isTrue();
    }
}

