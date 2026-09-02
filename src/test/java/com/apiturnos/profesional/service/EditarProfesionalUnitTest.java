package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.ProfesionalDuplicadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditarProfesionalUnitTest {

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private RegistradorAuditoria registradorAuditoria;

    @InjectMocks
    private EditarProfesional editarProfesional;

    private Profesional profesional;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Gómez");
        profesional.setEmail("carlos.gomez@test.com");
        profesional.setTelefono("+5491100001111");
        profesional.setEspecialidad("Odontología");
    }

    @Test
    @DisplayName("Edita profesional exitosamente y audita el evento")
    void editar_exitoso() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.save(any(Profesional.class))).thenAnswer(inv -> inv.getArgument(0));

        Profesional editado = editarProfesional.ejecutar(
                1L, "Carlos Alberto", "Gómez Pérez", "carlos.nuevo@test.com", "+5491199998888", "Ortodoncia", "admin");

        assertNotNull(editado);
        assertEquals("Carlos Alberto", editado.getNombre());
        assertEquals("Gómez Pérez", editado.getApellido());
        assertEquals("carlos.nuevo@test.com", editado.getEmail());
        assertEquals("+5491199998888", editado.getTelefono());
        assertEquals("Ortodoncia", editado.getEspecialidad());

        verify(registradorAuditoria).registrar(
                eq("PROFESIONAL"), eq("Profesional"), eq(1L),
                eq(OperacionAuditoria.UPDATE), eq("admin"), eq(1L), eq("Datos de profesional actualizados"));
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si el profesional no existe")
    void editar_noExiste_lanzaEntidadNoEncontradaException() {
        when(profesionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadNoEncontradaException.class, () ->
                editarProfesional.ejecutar(99L, "Carlos", "Gómez", "carlos@test.com", "+123", "Esp", "admin"));
    }

    @Test
    @DisplayName("Lanza ProfesionalDuplicadoException si el nuevo email ya le pertenece a otro profesional")
    void editar_emailDuplicadoEnOtroProfesional_lanzaExcepcion() {
        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));
        when(profesionalRepository.existsByEmailIgnoreCaseAndIdNot("otro@test.com", 1L)).thenReturn(true);

        assertThrows(ProfesionalDuplicadoException.class, () ->
                editarProfesional.ejecutar(1L, "Carlos", "Gómez", "otro@test.com", "+123", "Esp", "admin"));
    }

    @Test
    @DisplayName("Lanza NegocioException si los campos requeridos son inválidos")
    void editar_camposInvalidos_lanzaNegocioException() {
        assertThrows(NegocioException.class, () ->
                editarProfesional.ejecutar(null, "Carlos", "Gómez", "carlos@test.com", "+123", "Esp", "admin"));

        assertThrows(NegocioException.class, () ->
                editarProfesional.ejecutar(1L, "", "Gómez", "carlos@test.com", "+123", "Esp", "admin"));
    }
}
