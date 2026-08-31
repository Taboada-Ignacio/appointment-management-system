package com.apiturnos.turno.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelarTurno {

    private final TurnoRepository turnoRepository;
    private final MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public CancelarTurno(TurnoRepository turnoRepository,
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
    public Turno ejecutar(Long turnoId, String motivoTexto, String usuario) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));

        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo(motivoTexto);
        motivo = motivoBajaTurnoRepository.save(motivo);

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, "CANCELADO", usuario, motivoTexto, motivo);

        Long profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("TURNO", "Turno", turnoId,
                OperacionAuditoria.CANCEL, usuario, profesionalId, "Turno cancelado: " + motivoTexto);

        registradorNotificacion.registrarSiCorresponde(turno.getCliente(), turno,
                TipoNotificacion.CANCELACION_TURNO, "Su turno del " + turno.getDiaAgenda().getFecha() + " ha sido cancelado. Motivo: " + motivoTexto);

        return turno;
    }
}
