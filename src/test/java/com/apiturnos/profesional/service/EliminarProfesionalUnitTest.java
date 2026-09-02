package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.ProfesionalConDependenciasException;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EliminarProfesionalUnitTest {

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @Mock
    private RegistradorAuditoria registradorAuditoria;

    @InjectMocks
    private EliminarProfesional eliminarProfesional;

    private Profesional profesional;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Gómez");
    }

    @Test
    @DisplayName("Elimina profesional y su configuración si existe, auditando el evento")
    void eliminar_exitosoConConfiguracion() {
        Configuracion config = new Configuracion();
        config.setId(10L);
        config.setProfesional(profesional);

        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.of(config));

        eliminarProfesional.ejecutar(1L, "admin");

        verify(configuracionRepository).delete(config);
        verify(profesionalRepository).delete(profesional);
        verify(profesionalRepository).flush();
        verify(registradorAuditoria).registrar(
                eq("PROFESIONAL"), eq("Profesional"), eq(1L),
                eq(OperacionAuditoria.DELETE), eq("admin"), eq(1L), eq("Eliminación de profesional"));
    }

    @Test
    @DisplayName("Elimina profesional sin configuración asociada")
    void eliminar_exitosoSinConfiguracion() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());

        eliminarProfesional.ejecutar(1L, "admin");

        verify(profesionalRepository).delete(profesional);
        verify(profesionalRepository).flush();
        verify(registradorAuditoria).registrar(
                eq("PROFESIONAL"), eq("Profesional"), eq(1L),
                eq(OperacionAuditoria.DELETE), eq("admin"), eq(1L), eq("Eliminación de profesional"));
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si el profesional no existe")
    void eliminar_noExiste_lanzaExcepcion() {
        when(profesionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadNoEncontradaException.class, () ->
                eliminarProfesional.ejecutar(99L, "admin"));
    }

    @Test
    @DisplayName("Lanza NegocioException si el ID es nulo")
    void eliminar_idNulo_lanzaNegocioException() {
        assertThrows(NegocioException.class, () ->
                eliminarProfesional.ejecutar(null, "admin"));
    }

    @Test
    @DisplayName("Informa conflicto si el profesional conserva información asociada")
    void eliminar_conDependencias_lanzaConflicto() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(configuracionRepository.findByProfesionalId(1L)).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("restricción FK"))
                .when(profesionalRepository).flush();

        assertThrows(ProfesionalConDependenciasException.class, () ->
                eliminarProfesional.ejecutar(1L, "admin"));
    }
}
