package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.Turno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AplicarExcepcionAgenda {

    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final ProfesionalRepository profesionalRepository;
    private final ValidadorExcepcionAgenda validador;
    private final EvaluarImpactoExcepcionAgenda evaluarImpacto;
    private final RegistradorAuditoria registradorAuditoria;

    public AplicarExcepcionAgenda(ExcepcionAgendaRepository excepcionAgendaRepository,
                                  ProfesionalRepository profesionalRepository,
                                  ValidadorExcepcionAgenda validador,
                                  EvaluarImpactoExcepcionAgenda evaluarImpacto,
                                  RegistradorAuditoria registradorAuditoria) {
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.profesionalRepository = profesionalRepository;
        this.validador = validador;
        this.evaluarImpacto = evaluarImpacto;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId,
                                     LocalDate fechaInicio,
                                     LocalDate fechaFin,
                                     TipoExcepcion tipo,
                                     String motivo,
                                     String usuario) {
        return ejecutar(
                profesionalId,
                new SolicitudExcepcionAgenda(
                        fechaInicio, fechaFin, tipo, null, null, motivo),
                usuario);
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId,
                                     LocalDate fechaInicio,
                                     LocalDate fechaFin,
                                     TipoExcepcion tipo,
                                     LocalTime horaInicio,
                                     LocalTime horaFin,
                                     String motivo,
                                     String usuario) {
        return ejecutar(
                profesionalId,
                new SolicitudExcepcionAgenda(
                        fechaInicio, fechaFin, tipo, horaInicio, horaFin, motivo),
                usuario);
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId,
                                     LocalDate fechaInicio,
                                     LocalDate fechaFin,
                                     TipoExcepcion tipo,
                                     List<IntervaloHorario> brechas,
                                     String motivo,
                                     String usuario) {
        return ejecutar(
                profesionalId,
                new SolicitudExcepcionAgenda(
                        fechaInicio, fechaFin, tipo, brechas, motivo),
                usuario);
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId,
                                     SolicitudExcepcionAgenda solicitud,
                                     String usuario) {
        validador.validar(solicitud);
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));

        ExcepcionAgenda excepcion = new ExcepcionAgenda();
        excepcion.setProfesional(profesional);
        copiarDatos(excepcion, solicitud);
        excepcion.setActiva(true);
        excepcion = excepcionAgendaRepository.save(excepcion);

        List<Turno> afectados = evaluarImpacto.ejecutar(excepcion, usuario);

        registradorAuditoria.registrar(
                "AGENDA",
                "ExcepcionAgenda",
                excepcion.getId(),
                OperacionAuditoria.CREATE,
                usuario,
                profesionalId,
                "EXCEPCION_AGENDA_CREADA: tipo=" + excepcion.getTipo()
                        + "; desde=" + excepcion.getFechaInicio()
                        + "; hasta=" + excepcion.getFechaFin()
                        + "; turnosAfectados=" + ids(afectados));

        return excepcion;
    }

    static void copiarDatos(ExcepcionAgenda excepcion, SolicitudExcepcionAgenda solicitud) {
        excepcion.setFechaInicio(solicitud.fechaInicio());
        excepcion.setFechaFin(solicitud.fechaFin());
        excepcion.setTipo(solicitud.tipo());
        excepcion.setHoraInicio(solicitud.horaInicio());
        excepcion.setHoraFin(solicitud.horaFin());
        excepcion.setMotivo(solicitud.motivo().trim());

        excepcion.limpiarBrechas();
        if (solicitud.brechas() != null) {
            for (IntervaloHorario intervalo : solicitud.brechas()) {
                excepcion.agregarBrecha(intervalo.inicio(), intervalo.fin());
            }
        }
    }

    private String ids(List<Turno> turnos) {
        return turnos.stream().map(Turno::getId).toList().toString();
    }
}
