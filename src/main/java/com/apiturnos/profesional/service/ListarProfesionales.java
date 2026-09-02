package com.apiturnos.profesional.service;

import com.apiturnos.profesional.dto.ProfesionalResponseDto;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListarProfesionales {

    private final ProfesionalRepository profesionalRepository;

    public ListarProfesionales(ProfesionalRepository profesionalRepository) {
        this.profesionalRepository = profesionalRepository;
    }

    public List<ProfesionalResponseDto> ejecutar() {
        Sort orden = Sort.by(
                Sort.Order.asc("apellido").ignoreCase(),
                Sort.Order.asc("nombre").ignoreCase(),
                Sort.Order.asc("id"));
        return profesionalRepository.findAll(orden).stream()
                .map(ProfesionalResponseDto::new)
                .toList();
    }

    public Page<ProfesionalResponseDto> ejecutar(String busqueda, String especialidad, Pageable pageable) {
        String busquedaPattern = (busqueda != null && !busqueda.isBlank())
                ? "%" + busqueda.trim().toLowerCase() + "%"
                : null;
        String especialidadPattern = (especialidad != null && !especialidad.isBlank())
                ? "%" + especialidad.trim().toLowerCase() + "%"
                : null;

        Page<Profesional> page = profesionalRepository.buscar(busquedaPattern, especialidadPattern, pageable);
        return page.map(ProfesionalResponseDto::new);
    }
}
