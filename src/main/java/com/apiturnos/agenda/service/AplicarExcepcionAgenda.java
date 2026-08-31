package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class AplicarExcepcionAgenda {

    private static final Set<String> ESTADOS_AFECTABLES = Set.of(
            "ASIGNADO", "PENDIENTE_DE_APROBACION", "CONFIRMADO");

    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final TurnoRepository turnoRepository;
    private final MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    private final ProfesionalRepository profesionalRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public AplicarExcepcionAgenda(ExcepcionAgendaRepository excepcionAgendaRepository,
                                   DiaAgendaRepository diaAgendaRepository,
                                   TurnoRepository turnoRepository,
                                   MotivoBajaTurnoRepository motivoBajaTurnoRepository,
                                   ProfesionalRepository profesionalRepository,
                                   GestorCambioEstado gestorCambioEstado,
                                   RegistradorAuditoria registradorAuditoria,
                                   RegistradorNotificacion registradorNotificacion) {
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.turnoRepository = turnoRepository;
        this.motivoBajaTurnoRepository = motivoBajaTurnoRepository;
        this.profesionalRepository = profesionalRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId, LocalDate fechaInicio, LocalDate fechaFin,
                                     TipoExcepcion tipo, String motivo, String usuario) {
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));

        // Save exception
        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setProfesional(profesional);
        excepcion.setFechaInicio(fechaInicio);
        excepcion.setFechaFin(fechaFin);
        excepcion.setTipo(tipo);
        excepcion.setMotivo(motivo);
        excepcion = excepcionAgendaRepository.save(excepcion);

        // Find affected days
        List<DiaAgenda> diasAfectados = diaAgendaRepository
                .findByProfesionalIdAndFechaBetween(profesionalId, fechaInicio, fechaFin);

        // Create motivo for baja
        MotivoBajaTurno motivoBaja = new MotivoBajaTurno();
        motivoBaja.setMotivo("Excepción de agenda: " + tipo + " - " + motivo);
        motivoBaja = motivoBajaTurnoRepository.save(motivoBaja);

        // Process each affected day
        for (DiaAgenda dia : diasAfectados) {
            List<Turno> turnosDia = turnoRepository.findByDiaAgendaId(dia.getId());
            for (Turno turno : turnosDia) {
                String estadoTurno = gestorCambioEstado.obtenerNombreEstadoActual(
                        AmbitoEstado.TURNO, turno.getId());
                if (estadoTurno != null && ESTADOS_AFECTABLES.contains(estadoTurno)) {
                    gestorCambioEstado.registrarCambio(
                            AmbitoEstado.TURNO, turno.getId(), "DADO_DE_BAJA",
                            usuario, "Baja por excepción: " + motivo, motivoBaja);

                    registradorNotificacion.registrarSiCorresponde(
                            turno.getCliente(), turno, TipoNotificacion.BAJA_TURNO,
                            "Su turno del " + dia.getFecha() + " ha sido dado de baja. Motivo: " + motivo);
                }
            }
        }

        registradorAuditoria.registrar("AGENDA", "ExcepcionAgenda", excepcion.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalId,
                "Excepción aplicada: " + tipo + " del " + fechaInicio + " al " + fechaFin);

        return excepcion;
    }
}
