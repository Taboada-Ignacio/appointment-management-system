package com.apiturnos.atencion.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarTiposAtencion {

    private final TipoAtencionRepository tipoAtencionRepository;
    private final ProfesionalRepository profesionalRepository;

    public ListarTiposAtencion(TipoAtencionRepository tipoAtencionRepository,
                               ProfesionalRepository profesionalRepository) {
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.profesionalRepository = profesionalRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoAtencion> ejecutar(Long profesionalId, Boolean soloActivos) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }

        if (!profesionalRepository.existsById(profesionalId)) {
            throw new EntidadNoEncontradaException("Profesional", profesionalId);
        }

        if (Boolean.TRUE.equals(soloActivos)) {
            return tipoAtencionRepository.findByProfesionalIdAndActivoTrueOrderByIdAsc(profesionalId);
        } else {
            return tipoAtencionRepository.findByProfesionalIdOrderByIdAsc(profesionalId);
        }
    }
}

