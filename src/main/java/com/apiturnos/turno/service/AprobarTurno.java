package com.apiturnos.turno.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobarTurno {

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public AprobarTurno(TurnoRepository turnoRepository,
                        GestorCambioEstado gestorCambioEstado,
                        RegistradorAuditoria registradorAuditoria,
                        RegistradorNotificacion registradorNotificacion) {
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
    }

    @Transactional
    public Turno ejecutar(Long turnoId, String usuario) {
        Turno turno = turnoRepository.findByIdForUpdate(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, "ASIGNADO", usuario, "Turno aprobado", null);

        Long profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("TURNO", "Turno", turnoId,
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId, "Turno aprobado: PENDIENTE_DE_APROBACION → ASIGNADO");

        registradorNotificacion.registrarSiCorresponde(turno.getCliente(), turno,
                TipoNotificacion.CONFIRMACION_TURNO, "Su turno ha sido aprobado para el " + turno.getDiaAgenda().getFecha());

        return turno;
    }
}
