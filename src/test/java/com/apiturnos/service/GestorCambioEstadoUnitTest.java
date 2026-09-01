package com.apiturnos.service;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.model.Estado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.repository.EstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.TransicionEstadoInvalidaException;
import com.apiturnos.turno.service.PoliticaTransicionesTurno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestorCambioEstadoUnitTest {

    @Mock
    private EstadoRepository estadoRepository;

    @Mock
    private CambioEstadoRepository cambioEstadoRepository;

    @Spy
    private PoliticaTransicionesTurno politicaTransicionesTurno = new PoliticaTransicionesTurno();

    @InjectMocks
    private GestorCambioEstado gestorCambioEstado;

    private Estado estadoHabilitado;
    private Estado estadoInhabilitado;
    private Estado estadoDadoDeBaja;
    private Estado estadoAsignado;
    private Estado estadoReprogramado;
    private Estado estadoCancelado;

    @BeforeEach
    void setUp() {
        estadoHabilitado = new Estado();
        estadoHabilitado.setId(1L);
        estadoHabilitado.setNombre("HABILITADO");
        estadoHabilitado.setAmbito(AmbitoEstado.CLIENTE);

        estadoInhabilitado = new Estado();
        estadoInhabilitado.setId(2L);
        estadoInhabilitado.setNombre("INHABILITADO");
        estadoInhabilitado.setAmbito(AmbitoEstado.CLIENTE);

        estadoDadoDeBaja = new Estado();
        estadoDadoDeBaja.setId(3L);
        estadoDadoDeBaja.setNombre("DADO_DE_BAJA");
        estadoDadoDeBaja.setAmbito(AmbitoEstado.CLIENTE);

        estadoAsignado = new Estado();
        estadoAsignado.setId(10L);
        estadoAsignado.setNombre("ASIGNADO");
        estadoAsignado.setAmbito(AmbitoEstado.TURNO);

        estadoReprogramado = new Estado();
        estadoReprogramado.setId(11L);
        estadoReprogramado.setNombre("REPROGRAMADO");
        estadoReprogramado.setAmbito(AmbitoEstado.TURNO);

        estadoCancelado = new Estado();
        estadoCancelado.setId(12L);
        estadoCancelado.setNombre("CANCELADO");
        estadoCancelado.setAmbito(AmbitoEstado.TURNO);
    }

    @Test
    @DisplayName("Registrar cambio inicial guarda CambioEstado sin fecha fin")
    void testRegistrarCambioInicial() {
        when(estadoRepository.findByNombreAndAmbito("HABILITADO", AmbitoEstado.CLIENTE))
                .thenReturn(Optional.of(estadoHabilitado));
        when(cambioEstadoRepository.save(any(CambioEstado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CambioEstado resultado = gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.CLIENTE, 100L, "HABILITADO", "admin", "Alta cliente");

        assertThat(resultado.getEstado()).isEqualTo(estadoHabilitado);
        assertThat(resultado.getAmbito()).isEqualTo(AmbitoEstado.CLIENTE);
        assertThat(resultado.getEntidadId()).isEqualTo(100L);
        assertThat(resultado.getFechaHoraFin()).isNull();
        assertThat(resultado.getUsuario()).isEqualTo("admin");
        verify(cambioEstadoRepository, times(1)).save(any(CambioEstado.class));
    }

    @Test
    @DisplayName("Transición válida de HABILITADO a INHABILITADO finaliza estado anterior y crea nuevo")
    void testTransicionValidaCliente() {
        CambioEstado anterior = new CambioEstado();
        anterior.setId(1L);
        anterior.setEstado(estadoHabilitado);
        anterior.setAmbito(AmbitoEstado.CLIENTE);
        anterior.setEntidadId(100L);
        anterior.setFechaHoraInicio(Instant.now().minusSeconds(3600));

        when(cambioEstadoRepository.findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDescIdDesc(AmbitoEstado.CLIENTE, 100L))
                .thenReturn(Optional.of(anterior));
        when(estadoRepository.findByNombreAndAmbito("INHABILITADO", AmbitoEstado.CLIENTE))
                .thenReturn(Optional.of(estadoInhabilitado));
        when(cambioEstadoRepository.save(any(CambioEstado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CambioEstado nuevo = gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, 100L, "INHABILITADO", "admin", "Inasistencias", null);

        assertThat(anterior.getFechaHoraFin()).isNotNull();
        assertThat(nuevo.getEstado()).isEqualTo(estadoInhabilitado);
        assertThat(nuevo.getObservacion()).isEqualTo("Inasistencias");
    }

    @Test
    @DisplayName("Transición inválida lanza TransicionEstadoInvalidaException")
    void testTransicionInvalidaLanzaExcepcion() {
        CambioEstado actual = new CambioEstado();
        actual.setEstado(estadoReprogramado);
        actual.setAmbito(AmbitoEstado.TURNO);
        actual.setEntidadId(200L);

        when(cambioEstadoRepository.findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDescIdDesc(AmbitoEstado.TURNO, 200L))
                .thenReturn(Optional.of(actual));

        // REPROGRAMADO can only transition to ASIGNADO, not CANCELADO
        assertThatThrownBy(() -> gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, 200L, "CANCELADO", "admin", "Intento inválido", null))
                .isInstanceOf(TransicionEstadoInvalidaException.class);
    }

    @Test
    @DisplayName("Registrar cambio sobre entidad sin estado actual lanza EstadoInvalidoException")
    void testSinEstadoActualLanzaExcepcion() {
        when(cambioEstadoRepository.findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDescIdDesc(AmbitoEstado.CLIENTE, 999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, 999L, "HABILITADO", "admin", "Observacion", null))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    @DisplayName("Obtener historial delega correctamente al repositorio")
    void testObtenerHistorial() {
        when(cambioEstadoRepository.findByAmbitoAndEntidadIdOrderByFechaHoraInicioAscIdAsc(AmbitoEstado.CLIENTE, 100L))
                .thenReturn(List.of(new CambioEstado(), new CambioEstado()));

        List<CambioEstado> historial = gestorCambioEstado.obtenerHistorial(AmbitoEstado.CLIENTE, 100L);
        assertThat(historial).hasSize(2);
    }
}

