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

import java.util.Objects;

@Service
public class EditarTipoAtencion {

    private final TipoAtencionRepository tipoAtencionRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public EditarTipoAtencion(TipoAtencionRepository tipoAtencionRepository,
                              RegistradorAuditoria registradorAuditoria) {
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public TipoAtencion ejecutar(Long profesionalId, Long tipoAtencionId, String nombre,
                                  String descripcion, Integer duracionMinutos,
                                  Integer capacidadSimultanea, String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (tipoAtencionId == null) {
            throw new NegocioException("El ID del tipo de atención es obligatorio");
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

        TipoAtencion tipo = tipoAtencionRepository.findById(tipoAtencionId)
                .orElseThrow(() -> new EntidadNoEncontradaException("TipoAtencion", tipoAtencionId));

        if (!tipo.getProfesional().getId().equals(profesionalId)) {
            throw new TipoAtencionNoPerteneceProfesionalException(tipoAtencionId, profesionalId);
        }

        Integer capacidadAnterior = tipo.getCapacidadSimultanea();
        boolean cambioCapacidad = !Objects.equals(capacidadAnterior, capacidadSimultanea);

        tipo.setNombre(nombre.trim());
        tipo.setDescripcion(descripcion != null ? descripcion.trim() : null);
        tipo.setDuracionMinutos(duracionMinutos);
        tipo.setCapacidadSimultanea(capacidadSimultanea);
        tipo = tipoAtencionRepository.save(tipo);

        if (cambioCapacidad) {
            registradorAuditoria.registrar(
                    "TIPO_ATENCION", "TipoAtencion", tipo.getId(),
                    OperacionAuditoria.UPDATE, usuario, profesionalId,
                    "CAPACIDAD_TIPO_ATENCION_MODIFICADA: anterior=" + capacidadAnterior + ", nueva=" + capacidadSimultanea);
        }

        registradorAuditoria.registrar(
                "TIPO_ATENCION", "TipoAtencion", tipo.getId(),
                OperacionAuditoria.UPDATE, usuario, profesionalId,
                "TIPO_ATENCION_EDITADO: " + tipo.getNombre() + " (duracion=" + duracionMinutos + "m, capacidad=" + capacidadSimultanea + ")");

        return tipo;
    }
}

