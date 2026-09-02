package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.ProfesionalConDependenciasException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarProfesional {

    private final ProfesionalRepository profesionalRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public EliminarProfesional(ProfesionalRepository profesionalRepository,
                               ConfiguracionRepository configuracionRepository,
                               RegistradorAuditoria registradorAuditoria) {
        this.profesionalRepository = profesionalRepository;
        this.configuracionRepository = configuracionRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public void ejecutar(Long id, String usuario) {
        if (id == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }

        Profesional profesional = profesionalRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", id));

        // Si tiene una configuración asociada, se elimina primero por la restricción FK
        configuracionRepository.findByProfesionalId(id)
                .ifPresent(configuracionRepository::delete);

        try {
            profesionalRepository.delete(profesional);
            profesionalRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProfesionalConDependenciasException(id, ex);
        }

        registradorAuditoria.registrar("PROFESIONAL", "Profesional", id,
                OperacionAuditoria.DELETE, usuario, id, "Eliminación de profesional");
    }
}
