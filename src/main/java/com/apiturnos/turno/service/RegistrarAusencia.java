package com.apiturnos.turno.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class RegistrarAusencia {

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final Clock clock;

    public RegistrarAusencia(TurnoRepository turnoRepository,
                              GestorCambioEstado gestorCambioEstado,
                              RegistradorAuditoria registradorAuditoria,
                              Clock clock) {
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.clock = clock;
    }

    @Transactional
    public Turno ejecutar(Long turnoId, String usuario) {
        Turno turno = turnoRepository.findByIdForUpdate(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));

        if (clock.instant().isBefore(turno.getFinEstimado())) {
            throw new NegocioException(
                    "No se puede registrar ausencia antes del fin estimado del Turno " + turnoId);
        }

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, "NO_ASISTIO", usuario, "Ausencia registrada", null);

        Long profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("TURNO", "Turno", turnoId,
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId, "Ausencia registrada");

        return turno;
    }
}
