package com.apiturnos.atencion.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.CapacidadAgotadaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificarCapacidadTipoAtencionUnitTest {

    @Mock
    private TurnoRepository turnoRepository;

    @Mock
    private GestorCambioEstado gestorCambioEstado;

    private VerificarCapacidadTipoAtencion verificador;

    private TipoAtencion tipoCapacidad1;
    private TipoAtencion tipoCapacidad3;

    private final Instant baseTime = Instant.parse("2026-09-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        verificador = new VerificarCapacidadTipoAtencion(turnoRepository, gestorCambioEstado);

        tipoCapacidad1 = new TipoAtencion();
        tipoCapacidad1.setId(1L);
        tipoCapacidad1.setNombre("Consulta General");
        tipoCapacidad1.setCapacidadSimultanea(1);
        tipoCapacidad1.setDuracionMinutos(30);

        tipoCapacidad3 = new TipoAtencion();
        tipoCapacidad3.setId(2L);
        tipoCapacidad3.setNombre("Control Grupal");
        tipoCapacidad3.setCapacidadSimultanea(3);
        tipoCapacidad3.setDuracionMinutos(30);
    }

    private Turno crearTurnoMock(Long id, Instant inicio, Instant fin) {
        Turno t = new Turno();
        t.setId(id);
        t.setInicioEstimado(inicio);
        t.setFinEstimado(fin);
        return t;
    }

    @Test
    @DisplayName("Capacidad 1: 0 turnos existentes -> Disponible")
    void capacidad1_sinTurnos_disponible() {
        Instant inicio = baseTime;
        Instant fin = baseTime.plusSeconds(1800);

        when(turnoRepository.findTurnosSolapadosPorTipoAtencion(1L, inicio, fin))
                .thenReturn(List.of());

        VerificarCapacidadTipoAtencion.ResultadoCapacidad resultado =
                verificador.evaluar(tipoCapacidad1, inicio, fin, null);

        assertThat(resultado.disponible()).isTrue();
        assertThat(resultado.sobrecapacidad()).isFalse();
        assertThat(resultado.turnosConcurrentes()).isEqualTo(0);
    }

    @Test
    @DisplayName("Capacidad 1: 1 turno activo existente -> Sobrecapacidad detectada")
    void capacidad1_unTurnoActivo_sobrecapacidad() {
        Instant inicio = baseTime;
        Instant fin = baseTime.plusSeconds(1800);

        Turno t1 = crearTurnoMock(10L, inicio, fin);
        when(turnoRepository.findTurnosSolapadosPorTipoAtencion(1L, inicio, fin))
                .thenReturn(List.of(t1));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 10L))
                .thenReturn("ASIGNADO");

        VerificarCapacidadTipoAtencion.ResultadoCapacidad resultado =
                verificador.evaluar(tipoCapacidad1, inicio, fin, null);

        assertThat(resultado.disponible()).isFalse();
        assertThat(resultado.sobrecapacidad()).isTrue();
        assertThat(resultado.turnosConcurrentes()).isEqualTo(1);
    }

    @Test
    @DisplayName("Capacidad 3: permite 1, 2 y 3 turnos concurrentes; 4to detecta sobrecapacidad")
    void capacidad3_permiteHasta3Turnos() {
        Instant inicio = baseTime;
        Instant fin = baseTime.plusSeconds(1800);

        Turno t1 = crearTurnoMock(101L, inicio, fin);
        Turno t2 = crearTurnoMock(102L, inicio.plusSeconds(300), fin.plusSeconds(300));
        Turno t3 = crearTurnoMock(103L, inicio, fin);
        Turno t4 = crearTurnoMock(104L, inicio, fin);

        when(turnoRepository.findTurnosSolapadosPorTipoAtencion(2L, inicio, fin))
                .thenReturn(List.of(t1, t2, t3));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 101L)).thenReturn("ASIGNADO");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 102L)).thenReturn("CONFIRMADO");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 103L)).thenReturn("PENDIENTE_DE_APROBACION");

        VerificarCapacidadTipoAtencion.ResultadoCapacidad r3 =
                verificador.evaluar(tipoCapacidad3, inicio, fin, null);

        assertThat(r3.turnosConcurrentes()).isEqualTo(3);
        assertThat(r3.disponible()).isFalse();
        assertThat(r3.sobrecapacidad()).isTrue();

        // Con 4 turnos
        when(turnoRepository.findTurnosSolapadosPorTipoAtencion(2L, inicio, fin))
                .thenReturn(List.of(t1, t2, t3, t4));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 104L)).thenReturn("ASIGNADO");

        VerificarCapacidadTipoAtencion.ResultadoCapacidad r4 =
                verificador.evaluar(tipoCapacidad3, inicio, fin, null);

        assertThat(r4.turnosConcurrentes()).isEqualTo(4);
        assertThat(r4.sobrecapacidad()).isTrue();
    }

    @Test
    @DisplayName("Solapamiento parcial: 09:00-09:30 con 09:15-09:45 cuenta como concurrente")
    void solapamientoParcial_cuentaComoConcurrencia() {
        Instant inicio = Instant.parse("2026-09-01T09:00:00Z");
        Instant fin = Instant.parse("2026-09-01T09:30:00Z");

        // Turno existente 09:15-09:45
        Turno t1 = crearTurnoMock(201L,
                Instant.parse("2026-09-01T09:15:00Z"),
                Instant.parse("2026-09-01T09:45:00Z"));

        when(turnoRepository.findTurnosSolapadosPorTipoAtencion(1L, inicio, fin))
                .thenReturn(List.of(t1));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 201L)).thenReturn("ASIGNADO");

        VerificarCapacidadTipoAtencion.ResultadoCapacidad resultado =
                verificador.evaluar(tipoCapacidad1, inicio, fin, null);

        assertThat(resultado.turnosConcurrentes()).isEqualTo(1);
        assertThat(resultado.sobrecapacidad()).isTrue();
    }

    @Test
    @DisplayName("Turnos dados de baja o cancelados NO ocupan capacidad")
    void turnosInactivos_noOcupanCapacidad() {
        Instant inicio = baseTime;
        Instant fin = baseTime.plusSeconds(1800);

        Turno t1 = crearTurnoMock(301L, inicio, fin);
        Turno t2 = crearTurnoMock(302L, inicio, fin);

        when(turnoRepository.findTurnosSolapadosPorTipoAtencion(1L, inicio, fin))
                .thenReturn(List.of(t1, t2));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 301L)).thenReturn("DADO_DE_BAJA");
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 302L)).thenReturn("CANCELADO");

        VerificarCapacidadTipoAtencion.ResultadoCapacidad resultado =
                verificador.evaluar(tipoCapacidad1, inicio, fin, null);

        assertThat(resultado.turnosConcurrentes()).isEqualTo(0);
        assertThat(resultado.disponible()).isTrue();
        assertThat(resultado.sobrecapacidad()).isFalse();
    }

    @Test
    @DisplayName("Autogestión lanza CapacidadAgotadaException si la capacidad está completa")
    void autogestion_lanzaExcepcionSiCapacidadCompleta() {
        Instant inicio = baseTime;
        Instant fin = baseTime.plusSeconds(1800);

        Turno t1 = crearTurnoMock(401L, inicio, fin);
        when(turnoRepository.findTurnosSolapadosPorTipoAtencion(1L, inicio, fin))
                .thenReturn(List.of(t1));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, 401L)).thenReturn("ASIGNADO");

        assertThatThrownBy(() -> verificador.verificarCapacidadAutoGestion(tipoCapacidad1, inicio, fin))
                .isInstanceOf(CapacidadAgotadaException.class);
    }
}

