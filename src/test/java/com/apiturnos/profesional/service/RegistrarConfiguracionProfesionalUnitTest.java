package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarConfiguracionProfesionalUnitTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private RegistradorAuditoria registradorAuditoria;

    @InjectMocks
    private RegistrarConfiguracionProfesional registrarConfiguracionProfesional;

    private Profesional profesional;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Gómez");
        profesional.setEmail("carlos.gomez@test.com");
        profesional.setTelefono("+5491100001111");
    }

    @Test
    @DisplayName("Registra configuración con valores por defecto cuando no se pasan valores personalizados")
    void registrar_valoresPorDefecto_exitoso() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());
        when(configuracionRepository.save(any(Configuracion.class))).thenAnswer(invocation -> {
            Configuracion c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        Configuracion resultado = registrarConfiguracionProfesional.ejecutar(1L, null, null, null, null, "admin");

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(profesional, resultado.getProfesional());
        assertEquals(1, resultado.getCantidadMaxTurnosALaVez());
        assertEquals(30, resultado.getDuracionAproximadaPorTurno());
        assertFalse(resultado.getAgendaSoloManejadaPorProfesional());
        assertEquals(24, resultado.getUmbralCancelacionHoras());

        verify(registradorAuditoria).registrar(
                eq("PROFESIONAL"), eq("Configuracion"), eq(10L),
                eq(OperacionAuditoria.CREATE), eq("admin"), eq(1L),
                eq("Configuración registrada: maxTurnos=1, duracion=30, umbralCancelacionHoras=24"));
    }

    @Test
    @DisplayName("Registra configuración con valores personalizados")
    void registrar_valoresPersonalizados_exitoso() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());
        when(configuracionRepository.save(any(Configuracion.class))).thenAnswer(invocation -> {
            Configuracion c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        Configuracion resultado = registrarConfiguracionProfesional.ejecutar(1L, 2, 45, true, 12, "admin");

        assertNotNull(resultado);
        assertEquals(2, resultado.getCantidadMaxTurnosALaVez());
        assertEquals(45, resultado.getDuracionAproximadaPorTurno());
        assertTrue(resultado.getAgendaSoloManejadaPorProfesional());
        assertEquals(12, resultado.getUmbralCancelacionHoras());

        ArgumentCaptor<Configuracion> captor = ArgumentCaptor.forClass(Configuracion.class);
        verify(configuracionRepository).save(captor.capture());
        Configuracion guardada = captor.getValue();
        assertEquals(2, guardada.getCantidadMaxTurnosALaVez());
        assertEquals(45, guardada.getDuracionAproximadaPorTurno());
        assertTrue(guardada.getAgendaSoloManejadaPorProfesional());
        assertEquals(12, guardada.getUmbralCancelacionHoras());

        verify(registradorAuditoria).registrar(
                eq("PROFESIONAL"), eq("Configuracion"), eq(10L),
                eq(OperacionAuditoria.CREATE), eq("admin"), eq(1L),
                eq("Configuración registrada: maxTurnos=2, duracion=45, umbralCancelacionHoras=12"));
    }

    @Test
    @DisplayName("Lanza NegocioException si el ID del profesional es nulo")
    void registrar_profesionalIdNulo_lanzaNegocioException() {
        assertThrows(NegocioException.class, () ->
                registrarConfiguracionProfesional.ejecutar(null, 1, 30, false, 24, "admin"));
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si el profesional no existe")
    void registrar_profesionalNoExiste_lanzaEntidadNoEncontradaException() {
        when(profesionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadNoEncontradaException.class, () ->
                registrarConfiguracionProfesional.ejecutar(99L, 1, 30, false, 24, "admin"));
    }

    @Test
    @DisplayName("Lanza NegocioException si el profesional ya tiene una configuración registrada")
    void registrar_configuracionYaExiste_lanzaNegocioException() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.of(new Configuracion()));

        NegocioException ex = assertThrows(NegocioException.class, () ->
                registrarConfiguracionProfesional.ejecutar(1L, 1, 30, false, 24, "admin"));
        assertEquals("El profesional ya posee una configuración registrada", ex.getMessage());
    }

    @Test
    @DisplayName("Lanza NegocioException si cantidadMaxTurnos es menor o igual a 0")
    void registrar_cantidadMaxTurnosInvalida_lanzaNegocioException() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());

        assertThrows(NegocioException.class, () ->
                registrarConfiguracionProfesional.ejecutar(1L, 0, 30, false, 24, "admin"));

        assertThrows(NegocioException.class, () ->
                registrarConfiguracionProfesional.ejecutar(1L, -1, 30, false, 24, "admin"));
    }

    @Test
    @DisplayName("Lanza NegocioException si duracionAproximada es menor o igual a 0")
    void registrar_duracionInvalida_lanzaNegocioException() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());

        assertThrows(NegocioException.class, () ->
                registrarConfiguracionProfesional.ejecutar(1L, 1, 0, false, 24, "admin"));

        assertThrows(NegocioException.class, () ->
                registrarConfiguracionProfesional.ejecutar(1L, 1, -10, false, 24, "admin"));
    }

    @Test
    @DisplayName("Lanza EstadoInvalidoException si umbralCancelacionHoras es negativo")
    void registrar_umbralCancelacionNegativo_lanzaEstadoInvalidoException() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());

        assertThrows(EstadoInvalidoException.class, () ->
                registrarConfiguracionProfesional.ejecutar(1L, 1, 30, false, -1, "admin"));
    }
}

