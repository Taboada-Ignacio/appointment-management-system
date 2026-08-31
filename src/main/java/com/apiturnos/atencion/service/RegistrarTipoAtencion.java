package com.apiturnos.atencion.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarTipoAtencion {

    private final TipoAtencionRepository tipoAtencionRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public RegistrarTipoAtencion(TipoAtencionRepository tipoAtencionRepository,
                                  ProfesionalRepository profesionalRepository,
                                  RegistradorAuditoria registradorAuditoria) {
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.profesionalRepository = profesionalRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public TipoAtencion ejecutar(Long profesionalId, String nombre, String descripcion,
                                  Integer duracionMinutos, Integer capacidadSimultanea,
                                  String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre del tipo de atención es obligatorio");
        }
        if (duracionMinutos == null || duracionMinutos <= 0) {
            throw new NegocioException("La duración en minutos debe ser mayor a 0");
        }
        if (capacidadSimultanea == null || capacidadSimultanea < 1) {
            throw new NegocioException("La capacidad simultánea debe ser mayor o igual a 1");
        }

        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));

        TipoAtencion tipo = new TipoAtencion();
        tipo.setProfesional(profesional);
        tipo.setNombre(nombre.trim());
        tipo.setDescripcion(descripcion != null ? descripcion.trim() : null);
        tipo.setDuracionMinutos(duracionMinutos);
        tipo.setCapacidadSimultanea(capacidadSimultanea);
        tipo.setActivo(true);
        tipo = tipoAtencionRepository.save(tipo);

        registradorAuditoria.registrar(
                "TIPO_ATENCION", "TipoAtencion", tipo.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalId,
                "TIPO_ATENCION_CREADO: " + tipo.getNombre() + " (duracion=" + duracionMinutos + "m, capacidad=" + capacidadSimultanea + ")");

        return tipo;
    }
}

