package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.ProfesionalDuplicadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarProfesionalUnitTest {

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private RegistradorAuditoria registradorAuditoria;

    @InjectMocks
    private RegistrarProfesional registrarProfesional;

    private Profesional profesionalGuardado;

    @BeforeEach
    void setUp() {
        profesionalGuardado = new Profesional();
        profesionalGuardado.setId(1L);
        profesionalGuardado.setNombre("Carlos");
        profesionalGuardado.setApellido("Gómez");
        profesionalGuardado.setEmail("carlos.gomez@test.com");
        profesionalGuardado.setTelefono("+5491100001111");
        profesionalGuardado.setEspecialidad("Odontología");
    }

    @Test
    @DisplayName("Registra un profesional exitosamente sin crear configuración y audita el evento")
    void registrar_exitoso() {
        when(profesionalRepository.existsByEmailIgnoreCase("carlos.gomez@test.com")).thenReturn(false);
        when(profesionalRepository.save(any(Profesional.class))).thenReturn(profesionalGuardado);

        Profesional resultado = registrarProfesional.ejecutar(
                "Carlos", "Gómez", "carlos.gomez@test.com", "+5491100001111", "Odontología", "admin");

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Carlos", resultado.getNombre());
        assertEquals("Gómez", resultado.getApellido());
        assertEquals("carlos.gomez@test.com", resultado.getEmail());

        ArgumentCaptor<Profesional> captor = ArgumentCaptor.forClass(Profesional.class);
        verify(profesionalRepository).save(captor.capture());
        Profesional guardado = captor.getValue();
        assertEquals("Carlos", guardado.getNombre());
        assertEquals("carlos.gomez@test.com", guardado.getEmail());

        verify(registradorAuditoria).registrar(
                eq("PROFESIONAL"), eq("Profesional"), eq(1L),
                eq(OperacionAuditoria.CREATE), eq("admin"), eq(1L), eq("Alta de profesional"));
    }

    @Test
    @DisplayName("Lanza ProfesionalDuplicadoException si el email ya existe")
    void registrar_emailDuplicado_lanzaExcepcion() {
        when(profesionalRepository.existsByEmailIgnoreCase("carlos.gomez@test.com")).thenReturn(true);

        assertThrows(ProfesionalDuplicadoException.class, () ->
                registrarProfesional.ejecutar("Carlos", "Gómez", "carlos.gomez@test.com",
                        "+5491100001111", "Odontología", "admin"));
    }

    @Test
    @DisplayName("Lanza NegocioException si los campos obligatorios son nulos o vacíos")
    void registrar_camposInvalidos_lanzaNegocioException() {
        assertThrows(NegocioException.class, () ->
                registrarProfesional.ejecutar("", "Gómez", "email@test.com", "123", "Esp", "admin"));

        assertThrows(NegocioException.class, () ->
                registrarProfesional.ejecutar("Carlos", null, "email@test.com", "123", "Esp", "admin"));

        assertThrows(NegocioException.class, () ->
                registrarProfesional.ejecutar("Carlos", "Gómez", "   ", "123", "Esp", "admin"));

        assertThrows(NegocioException.class, () ->
                registrarProfesional.ejecutar("Carlos", "Gómez", "email@test.com", "", "Esp", "admin"));
    }
}
