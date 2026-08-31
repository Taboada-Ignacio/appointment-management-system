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

@Service
public class RegistrarAusencia {

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public RegistrarAusencia(TurnoRepository turnoRepository,
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

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, "NO_ASISTIO", usuario, "Ausencia registrada", null);

        Long profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("TURNO", "Turno", turnoId,
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId, "Ausencia registrada");

        return turno;
    }
}
