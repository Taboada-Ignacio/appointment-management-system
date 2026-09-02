package com.apiturnos.profesional.service;

import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerProfesionalUnitTest {

    @Mock
    private ProfesionalRepository profesionalRepository;

    @InjectMocks
    private ObtenerProfesional obtenerProfesional;

    @Test
    @DisplayName("Obtiene profesional por ID exitosamente")
    void obtener_exitoso() {
        Profesional profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");

        when(profesionalRepository.findById(1L)).thenReturn(Optional.of(profesional));

        Profesional res = obtenerProfesional.ejecutar(1L);
        assertNotNull(res);
        assertEquals(1L, res.getId());
        assertEquals("Carlos", res.getNombre());
    }

    @Test
    @DisplayName("Lanza EntidadNoEncontradaException si no existe")
    void obtener_noExiste_lanzaExcepcion() {
        when(profesionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadNoEncontradaException.class, () -> obtenerProfesional.ejecutar(99L));
    }

    @Test
    @DisplayName("Lanza NegocioException si ID es nulo")
    void obtener_idNulo_lanzaNegocioException() {
        assertThrows(NegocioException.class, () -> obtenerProfesional.ejecutar(null));
    }
}

