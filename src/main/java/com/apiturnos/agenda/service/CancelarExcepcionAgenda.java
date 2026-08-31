package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelarExcepcionAgenda {

    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public CancelarExcepcionAgenda(ExcepcionAgendaRepository excepcionAgendaRepository,
                                    RegistradorAuditoria registradorAuditoria) {
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId, Long excepcionId, String usuario) {
        ExcepcionAgenda excepcion = excepcionAgendaRepository
                .findByIdAndProfesionalId(excepcionId, profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("ExcepcionAgenda", excepcionId));

        if (excepcion.isActiva()) {
            excepcion.setActiva(false);
            excepcion = excepcionAgendaRepository.save(excepcion);
            registradorAuditoria.registrar(
                    "AGENDA",
                    "ExcepcionAgenda",
                    excepcion.getId(),
                    OperacionAuditoria.CANCEL,
                    usuario,
                    profesionalId,
                    "EXCEPCION_AGENDA_CANCELADA: tipo=" + excepcion.getTipo()
                            + "; los turnos dados de baja no se reactivan automáticamente");
        }

        return excepcion;
    }
}
