package com.apiturnos.profesional.service;

import com.apiturnos.profesional.dto.ProfesionalResponseDto;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ListarProfesionalesUnitTest {

    @Mock
    private ProfesionalRepository profesionalRepository;

    @InjectMocks
    private ListarProfesionales listarProfesionales;

    @Test
    @DisplayName("Lista todos los profesionales sin parámetros")
    void listar_sinParametros_retornaTodos() {
        Profesional profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Gómez");

        when(profesionalRepository.findAll(any(Sort.class))).thenReturn(List.of(profesional));

        List<ProfesionalResponseDto> resultado = listarProfesionales.ejecutar();

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
    }

    @Test
    @DisplayName("Lista profesionales paginados correctamente con filtros")
    void listar_conFiltros_exitoso() {
        Profesional profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Carlos");
        profesional.setApellido("Gómez");
        profesional.setEmail("carlos@test.com");
        profesional.setEspecialidad("Odontología");

        Pageable pageable = PageRequest.of(0, 10);
        Page<Profesional> page = new PageImpl<>(List.of(profesional), pageable, 1);

        when(profesionalRepository.buscar(eq("%carlos%"), eq("%odontología%"), eq(pageable)))
                .thenReturn(page);

        Page<ProfesionalResponseDto> resultado = listarProfesionales.ejecutar("Carlos", "Odontología", pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals("Carlos", resultado.getContent().get(0).getNombre());
    }
}
