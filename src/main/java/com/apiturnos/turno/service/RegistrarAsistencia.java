package com.apiturnos.turno.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistrarAsistencia {

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public RegistrarAsistencia(TurnoRepository turnoRepository,
                                GestorCambioEstado gestorCambioEstado,
                                RegistradorAuditoria registradorAuditoria) {
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Turno ejecutar(Long turnoId, String usuario) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));

        turno.setInicioReal(Instant.now());
        turno = turnoRepository.save(turno);

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, "COMPLETADO", usuario, "Asistencia registrada", null);

        turno.setFinReal(Instant.now());
        turno = turnoRepository.save(turno);

        Long profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("TURNO", "Turno", turnoId,
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId, "Asistencia registrada");

        return turno;
    }
}
