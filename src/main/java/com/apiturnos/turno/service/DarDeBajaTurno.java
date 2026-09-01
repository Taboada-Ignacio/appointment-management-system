package com.apiturnos.turno.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.TurnoNoPerteneceProfesionalException;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DarDeBajaTurno {

    private final TurnoRepository turnoRepository;
    private final MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public DarDeBajaTurno(TurnoRepository turnoRepository,
                          MotivoBajaTurnoRepository motivoBajaTurnoRepository,
                          GestorCambioEstado gestorCambioEstado,
                          RegistradorAuditoria registradorAuditoria,
                          RegistradorNotificacion registradorNotificacion) {
        this.turnoRepository = turnoRepository;
        this.motivoBajaTurnoRepository = motivoBajaTurnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
    }

    @Transactional
    public Turno ejecutar(Long profesionalId, Long turnoId, String motivoTexto, String usuario) {
        if (motivoTexto == null || motivoTexto.isBlank()) {
            throw new EstadoInvalidoException("Dar de baja un Turno requiere un motivo");
        }
        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo(motivoTexto.trim());
        motivo = motivoBajaTurnoRepository.save(motivo);
        return ejecutar(profesionalId, turnoId, motivo, motivoTexto.trim(), usuario);
    }

    @Transactional
    public Turno ejecutar(Long turnoId, String motivoTexto, String usuario) {
        return ejecutar(null, turnoId, motivoTexto, usuario);
    }

    @Transactional
    public Turno ejecutar(Long profesionalId, Long turnoId, MotivoBajaTurno motivo,
                          String observacion, String usuario) {
        if (motivo == null) {
            throw new EstadoInvalidoException("Dar de baja un Turno requiere MotivoBajaTurno");
        }

        Turno turno = turnoRepository.findByIdForUpdate(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));

        Long profesionalIdTurno = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        if (profesionalId != null && !profesionalIdTurno.equals(profesionalId)) {
            throw new TurnoNoPerteneceProfesionalException(turnoId, profesionalId);
        }

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, PoliticaTransicionesTurno.DADO_DE_BAJA,
                usuario, observacion, motivo);

        registradorAuditoria.registrar(
                "TURNO", "Turno", turnoId, OperacionAuditoria.STATE_CHANGE,
                usuario, profesionalIdTurno, "TURNO_DADO_DE_BAJA: " + observacion);

        registradorNotificacion.registrarSiCorresponde(
                turno.getCliente(), turno, TipoNotificacion.BAJA_TURNO,
                "Su turno del " + turno.getDiaAgenda().getFecha()
                        + " ha sido dado de baja. Motivo: " + motivo.getMotivo());
        return turno;
    }

    @Transactional
    public Turno ejecutar(Long turnoId, MotivoBajaTurno motivo,
                          String observacion, String usuario) {
        return ejecutar(null, turnoId, motivo, observacion, usuario);
    }
}
