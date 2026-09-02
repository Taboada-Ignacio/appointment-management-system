package com.apiturnos.profesional.service;

import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ObtenerConfiguracionProfesional {

    private final ConfiguracionRepository configuracionRepository;
    private final ProfesionalRepository profesionalRepository;

    public ObtenerConfiguracionProfesional(ConfiguracionRepository configuracionRepository,
                                          ProfesionalRepository profesionalRepository) {
        this.configuracionRepository = configuracionRepository;
        this.profesionalRepository = profesionalRepository;
    }

    public Configuracion ejecutar(Long profesionalId) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (!profesionalRepository.existsById(profesionalId)) {
            throw new EntidadNoEncontradaException("Profesional", profesionalId);
        }
        return configuracionRepository.findByProfesionalId(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Configuración de Profesional", profesionalId));
    }
}

