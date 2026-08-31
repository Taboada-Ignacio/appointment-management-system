package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.model.TurnoHistorial;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReprogramarTurno {

    private final TurnoRepository turnoRepository;
    private final TurnoHistorialRepository turnoHistorialRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public ReprogramarTurno(TurnoRepository turnoRepository,
                            TurnoHistorialRepository turnoHistorialRepository,
                            DiaAgendaRepository diaAgendaRepository,
                            GestorCambioEstado gestorCambioEstado,
                            RegistradorAuditoria registradorAuditoria,
                            RegistradorNotificacion registradorNotificacion) {
        this.turnoRepository = turnoRepository;
        this.turnoHistorialRepository = turnoHistorialRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
    }

    @Transactional
    public Turno ejecutar(Long turnoId, Long nuevoDiaAgendaId, Instant nuevoInicio,
                           Instant nuevoFin, String motivo, String usuario) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));

        DiaAgenda nuevoDia = diaAgendaRepository.findById(nuevoDiaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", nuevoDiaAgendaId));

        // Save history before changes
        TurnoHistorial historial = new TurnoHistorial();
        historial.setTurno(turno);
        historial.setDiaAgendaAnterior(turno.getDiaAgenda());
        historial.setInicioEstimadoAnterior(turno.getInicioEstimado());
        historial.setFinEstimadoAnterior(turno.getFinEstimado());
        historial.setInicioEstimadoNuevo(nuevoInicio);
        historial.setFinEstimadoNuevo(nuevoFin);
        historial.setMotivo(motivo);
        historial.setUsuario(usuario);
        turnoHistorialRepository.save(historial);

        // ASIGNADO → REPROGRAMADO
        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, "REPROGRAMADO", usuario, motivo, null);

        // Update turno data
        turno.setDiaAgenda(nuevoDia);
        turno.setInicioEstimado(nuevoInicio);
        turno.setFinEstimado(nuevoFin);
        turno = turnoRepository.save(turno);

        // REPROGRAMADO → ASIGNADO
        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, "ASIGNADO", usuario, "Reprogramación completada", null);

        Long profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        registradorAuditoria.registrar("TURNO", "Turno", turnoId,
                OperacionAuditoria.RESCHEDULE, usuario, profesionalId,
                "Turno reprogramado. Motivo: " + motivo);

        registradorNotificacion.registrarSiCorresponde(turno.getCliente(), turno,
                TipoNotificacion.REPROGRAMACION_TURNO,
                "Su turno ha sido reprogramado para el " + nuevoDia.getFecha() + ". Motivo: " + motivo);

        return turno;
    }
}
