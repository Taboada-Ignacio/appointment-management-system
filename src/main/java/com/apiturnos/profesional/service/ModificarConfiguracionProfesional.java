package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModificarConfiguracionProfesional {

    private final ConfiguracionRepository configuracionRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public ModificarConfiguracionProfesional(ConfiguracionRepository configuracionRepository,
                                              ProfesionalRepository profesionalRepository,
                                              RegistradorAuditoria registradorAuditoria) {
        this.configuracionRepository = configuracionRepository;
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Configuracion ejecutar(Long profesionalId, Integer cantidadMaxTurnos,
                                   Integer duracionAproximada, Boolean agendaSoloManejadaPorProfesional,
                                   String usuario) {
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));

        Configuracion config = configuracionRepository.findByProfesionalId(profesionalId)
                .orElseGet(() -> {
                    Configuracion nueva = new Configuracion();
                    nueva.setProfesional(profesional);
                    return nueva;
                });

        if (cantidadMaxTurnos != null) {
            config.setCantidadMaxTurnosALaVez(cantidadMaxTurnos);
        }
        if (duracionAproximada != null) {
            config.setDuracionAproximadaPorTurno(duracionAproximada);
        }
        if (agendaSoloManejadaPorProfesional != null) {
            config.setAgendaSoloManejadaPorProfesional(agendaSoloManejadaPorProfesional);
        }

        config = configuracionRepository.save(config);

        registradorAuditoria.registrar("PROFESIONAL", "Configuracion", config.getId(),
                OperacionAuditoria.UPDATE, usuario, profesionalId,
                "Configuración actualizada: maxTurnos=" + config.getCantidadMaxTurnosALaVez() +
                ", duracion=" + config.getDuracionAproximadaPorTurno());

        return config;
    }
}
