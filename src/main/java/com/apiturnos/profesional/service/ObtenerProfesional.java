package com.apiturnos.profesional.service;

import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ObtenerProfesional {

    private final ProfesionalRepository profesionalRepository;

    public ObtenerProfesional(ProfesionalRepository profesionalRepository) {
        this.profesionalRepository = profesionalRepository;
    }

    public Profesional ejecutar(Long id) {
        if (id == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        return profesionalRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", id));
    }
}

