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
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.shared.exception.CapacidadAgotadaException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobarTurno {

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;
    private final DiaAgendaRepository diaAgendaRepository;
    private final VerificarCapacidadTipoAtencion verificarCapacidad;

    public AprobarTurno(TurnoRepository turnoRepository,
                        GestorCambioEstado gestorCambioEstado,
                        RegistradorAuditoria registradorAuditoria,
                        RegistradorNotificacion registradorNotificacion) {
        this(turnoRepository, gestorCambioEstado, registradorAuditoria, registradorNotificacion, null, null);
    }

    @Autowired
    public AprobarTurno(TurnoRepository turnoRepository,
                        GestorCambioEstado gestorCambioEstado,
                        RegistradorAuditoria registradorAuditoria,
                        RegistradorNotificacion registradorNotificacion,
                        DiaAgendaRepository diaAgendaRepository,
                        VerificarCapacidadTipoAtencion verificarCapacidad) {
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
        this.diaAgendaRepository = diaAgendaRepository;
        this.verificarCapacidad = verificarCapacidad;
    }

    @Transactional
    public Turno ejecutar(Long turnoId, String usuario) {
        return ejecutar(turnoId, usuario, false);
    }

    @Transactional
    public Turno ejecutar(Long turnoId, String usuario, boolean confirmarSobrecapacidad) {
        Turno referencia = turnoRepository.findByIdConRelaciones(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));
        if (diaAgendaRepository != null) {
            diaAgendaRepository.findByIdForUpdate(referencia.getDiaAgenda().getId());
        }
        Turno turno = turnoRepository.findByIdForUpdate(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));
        if (verificarCapacidad != null && turno.getTipoAtencion() != null) {
            var capacidad = verificarCapacidad.evaluar(turno.getTipoAtencion(),
                    turno.getInicioEstimado(), turno.getFinEstimado(), turnoId);
            if (capacidad.sobrecapacidad() && !confirmarSobrecapacidad) {
                throw new CapacidadAgotadaException(capacidad.turnosConcurrentes(), capacidad.capacidadMaxima());
            }
        }

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
