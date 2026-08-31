package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.ExcepcionAgendaInvalidaException;
import com.apiturnos.turno.model.Turno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModificarExcepcionAgenda {

    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final ValidadorExcepcionAgenda validador;
    private final EvaluarImpactoExcepcionAgenda evaluarImpacto;
    private final RegistradorAuditoria registradorAuditoria;

    public ModificarExcepcionAgenda(ExcepcionAgendaRepository excepcionAgendaRepository,
                                     ValidadorExcepcionAgenda validador,
                                     EvaluarImpactoExcepcionAgenda evaluarImpacto,
                                     RegistradorAuditoria registradorAuditoria) {
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.validador = validador;
        this.evaluarImpacto = evaluarImpacto;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId,
                                     Long excepcionId,
                                     SolicitudExcepcionAgenda solicitud,
                                     String usuario) {
        validador.validar(solicitud);
        ExcepcionAgenda excepcion = excepcionAgendaRepository
                .findByIdAndProfesionalId(excepcionId, profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("ExcepcionAgenda", excepcionId));
        if (!excepcion.isActiva()) {
            throw new ExcepcionAgendaInvalidaException("No se puede modificar una excepción cancelada");
        }

        AplicarExcepcionAgenda.copiarDatos(excepcion, solicitud);
        excepcion = excepcionAgendaRepository.save(excepcion);

        List<Turno> afectados = evaluarImpacto.ejecutar(excepcion, usuario);
        registradorAuditoria.registrar(
                "AGENDA",
                "ExcepcionAgenda",
                excepcion.getId(),
                OperacionAuditoria.UPDATE,
                usuario,
                profesionalId,
                "EXCEPCION_AGENDA_MODIFICADA: tipo=" + excepcion.getTipo()
                        + "; desde=" + excepcion.getFechaInicio()
                        + "; hasta=" + excepcion.getFechaFin()
                        + "; turnosAfectados="
                        + afectados.stream().map(Turno::getId).toList());

        return excepcion;
    }
}
