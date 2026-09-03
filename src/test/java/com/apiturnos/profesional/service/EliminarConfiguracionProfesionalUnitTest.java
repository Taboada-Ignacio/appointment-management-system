package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminarConfiguracionProfesionalUnitTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private RegistradorAuditoria registradorAuditoria;

    @InjectMocks
    private EliminarConfiguracionProfesional eliminarConfiguracionProfesional;

    private Profesional profesional;
    private Configuracion configuracion;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");

        configuracion = new Configuracion();
        configuracion.setId(10L);
        configuracion.setProfesional(profesional);
    }

    @Test
    @DisplayName("Elimina la configuración exitosamente y registra auditoría")
    void eliminar_exitoso() {
        when(profesionalRepository.existsById(1L)).thenReturn(true);
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.of(configuracion));

        eliminarConfiguracionProfesional.ejecutar(1L, "admin");

        verify(configuracionRepository).delete(configuracion);
        verify(registradorAuditoria).registrar(
                eq("PROFESIONAL"), eq("Configuracion"), eq(10L),
                eq(OperacionAuditoria.DELETE), eq("admin"), eq(1L),
                eq("Configuración eliminada (pruebas)"));
    }

    @Test
    @DisplayName("Lanza NegocioException si profesionalId es nulo")
    void eliminar_idNulo_lanzaNegocioException() {
        assertThrows(NegocioException.class, () ->
                eliminarConfiguracionProfesional.ejecutar(null, "admin"));
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si profesional no existe")
    void eliminar_profesionalNoExiste_lanzaEntidadNoEncontradaException() {
        when(profesionalRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntidadNoEncontradaException.class, () ->
                eliminarConfiguracionProfesional.ejecutar(99L, "admin"));
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si la configuración no existe")
    void eliminar_configuracionNoExiste_lanzaEntidadNoEncontradaException() {
        when(profesionalRepository.existsById(1L)).thenReturn(true);
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());

        assertThrows(EntidadNoEncontradaException.class, () ->
                eliminarConfiguracionProfesional.ejecutar(1L, "admin"));
    }
}

