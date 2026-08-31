package com.apiturnos.atencion.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerTipoAtencion {

    private final TipoAtencionRepository tipoAtencionRepository;

    public ObtenerTipoAtencion(TipoAtencionRepository tipoAtencionRepository) {
        this.tipoAtencionRepository = tipoAtencionRepository;
    }

    @Transactional(readOnly = true)
    public TipoAtencion ejecutar(Long profesionalId, Long tipoAtencionId) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (tipoAtencionId == null) {
            throw new NegocioException("El ID del tipo de atención es obligatorio");
        }

        TipoAtencion tipo = tipoAtencionRepository.findById(tipoAtencionId)
                .orElseThrow(() -> new EntidadNoEncontradaException("TipoAtencion", tipoAtencionId));

        if (!tipo.getProfesional().getId().equals(profesionalId)) {
            throw new TipoAtencionNoPerteneceProfesionalException(tipoAtencionId, profesionalId);
        }

        return tipo;
    }
}

