package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminarConfiguracionProfesional {

    private final ConfiguracionRepository configuracionRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public EliminarConfiguracionProfesional(ConfiguracionRepository configuracionRepository,
                                           ProfesionalRepository profesionalRepository,
                                           RegistradorAuditoria registradorAuditoria) {
        this.configuracionRepository = configuracionRepository;
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public void ejecutar(Long profesionalId, String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }

        if (!profesionalRepository.existsById(profesionalId)) {
            throw new EntidadNoEncontradaException("Profesional", profesionalId);
        }

        Configuracion configuracion = configuracionRepository.findByProfesionalId(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Configuración de Profesional", profesionalId));

        configuracionRepository.delete(configuracion);

        registradorAuditoria.registrar("PROFESIONAL", "Configuracion", configuracion.getId(),
                OperacionAuditoria.DELETE, usuario, profesionalId,
                "Configuración eliminada (pruebas)");
    }
}

