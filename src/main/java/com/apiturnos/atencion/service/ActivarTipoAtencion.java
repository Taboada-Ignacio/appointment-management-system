package com.apiturnos.atencion.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivarTipoAtencion {

    private final TipoAtencionRepository tipoAtencionRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public ActivarTipoAtencion(TipoAtencionRepository tipoAtencionRepository,
                               RegistradorAuditoria registradorAuditoria) {
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public TipoAtencion ejecutar(Long profesionalId, Long tipoAtencionId, String usuario) {
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

        tipo.setActivo(true);
        tipo = tipoAtencionRepository.save(tipo);

        registradorAuditoria.registrar(
                "TIPO_ATENCION", "TipoAtencion", tipo.getId(),
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId,
                "TIPO_ATENCION_ACTIVADO: " + tipo.getNombre());

        return tipo;
    }
}

