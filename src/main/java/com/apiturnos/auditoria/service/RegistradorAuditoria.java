package com.apiturnos.auditoria.service;

import com.apiturnos.auditoria.model.AuditoriaEvento;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import org.springframework.stereotype.Service;

@Service
public class RegistradorAuditoria {

    private final AuditoriaEventoRepository auditoriaEventoRepository;

    public RegistradorAuditoria(AuditoriaEventoRepository auditoriaEventoRepository) {
        this.auditoriaEventoRepository = auditoriaEventoRepository;
    }

    public AuditoriaEvento registrar(String modulo, String entidad, Long entidadId,
                                      OperacionAuditoria operacion, String usuario,
                                      Long profesionalId, String detalles) {
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setModulo(modulo);
        evento.setEntidad(entidad);
        evento.setEntidadId(entidadId.toString());
        evento.setOperacion(operacion);
        evento.setUsuario(usuario);
        evento.setProfesionalId(profesionalId);
        evento.setDetalles(detalles);
        return auditoriaEventoRepository.save(evento);
    }
}
