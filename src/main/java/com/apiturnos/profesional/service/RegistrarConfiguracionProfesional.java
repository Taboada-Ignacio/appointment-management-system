package com.apiturnos.profesional.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarConfiguracionProfesional {

    private final ConfiguracionRepository configuracionRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public RegistrarConfiguracionProfesional(ConfiguracionRepository configuracionRepository,
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
        return ejecutar(profesionalId, cantidadMaxTurnos, duracionAproximada,
                agendaSoloManejadaPorProfesional, null, usuario);
    }

    @Transactional
    public Configuracion ejecutar(Long profesionalId, Integer cantidadMaxTurnos,
                                  Integer duracionAproximada, Boolean agendaSoloManejadaPorProfesional,
                                  Integer umbralCancelacionHoras, String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }

        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));

        if (configuracionRepository.findByProfesionalId(profesionalId).isPresent()) {
            throw new NegocioException("El profesional ya posee una configuración registrada");
        }

        if (cantidadMaxTurnos != null && cantidadMaxTurnos <= 0) {
            throw new NegocioException("La cantidad máxima de turnos a la vez debe ser mayor a 0");
        }
        if (duracionAproximada != null && duracionAproximada <= 0) {
            throw new NegocioException("La duración aproximada por turno debe ser mayor a 0");
        }
        if (umbralCancelacionHoras != null && umbralCancelacionHoras < 0) {
            throw new EstadoInvalidoException("El umbral de cancelación no puede ser negativo");
        }

        Configuracion config = new Configuracion();
        config.setProfesional(profesional);
        if (cantidadMaxTurnos != null) {
            config.setCantidadMaxTurnosALaVez(cantidadMaxTurnos);
        }
        if (duracionAproximada != null) {
            config.setDuracionAproximadaPorTurno(duracionAproximada);
        }
        if (agendaSoloManejadaPorProfesional != null) {
            config.setAgendaSoloManejadaPorProfesional(agendaSoloManejadaPorProfesional);
        }
        if (umbralCancelacionHoras != null) {
            config.setUmbralCancelacionHoras(umbralCancelacionHoras);
        }

        config = configuracionRepository.save(config);

        registradorAuditoria.registrar("PROFESIONAL", "Configuracion", config.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalId,
                "Configuración registrada: maxTurnos=" + config.getCantidadMaxTurnosALaVez() +
                ", duracion=" + config.getDuracionAproximadaPorTurno() +
                ", umbralCancelacionHoras=" + config.getUmbralCancelacionHoras());

        return config;
    }
}

