package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.turno.model.Turno;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Component
public class ValidadorReprogramacionTurno {

    private static final Set<String> ESTADOS_DIA_VALIDOS = Set.of("ACTIVO", "EN_TRANSCURSO");
    private static final Set<String> ESTADOS_CLIENTE_VALIDOS = Set.of("HABILITADO", "REQUIERE_APROBACION");

    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad;
    private final VerificarCapacidadTipoAtencion verificadorCapacidadTipoAtencion;
    private final VerificadorCapacidad verificadorCapacidad;
    private final ConfiguracionRepository configuracionRepository;
    private final Clock clock;

    public ValidadorReprogramacionTurno(
            BrechaHorariaRepository brechaHorariaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            GestorCambioEstado gestorCambioEstado,
            EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad,
            VerificarCapacidadTipoAtencion verificadorCapacidadTipoAtencion,
            VerificadorCapacidad verificadorCapacidad,
            ConfiguracionRepository configuracionRepository,
            Clock clock) {
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.evaluadorDisponibilidad = evaluadorDisponibilidad;
        this.verificadorCapacidadTipoAtencion = verificadorCapacidadTipoAtencion;
        this.verificadorCapacidad = verificadorCapacidad;
        this.configuracionRepository = configuracionRepository;
        this.clock = clock;
    }

    public void validar(Turno turno, DiaAgenda nuevoDia, Instant nuevoInicio, Instant nuevoFin) {
        Long turnoId = turno.getId();
        if (nuevoInicio == null || nuevoFin == null || !nuevoInicio.isBefore(nuevoFin)) {
            rechazar(turnoId, "el horario destino es invalido");
        }

        Long profesionalActual = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        Long profesionalDestino = nuevoDia.getMesAgenda().getAgendaAnual().getProfesional().getId();
        if (!profesionalActual.equals(profesionalDestino)) {
            rechazar(turnoId, "el dia destino pertenece a otro profesional");
        }
        if (!profesionalActual.equals(turno.getCliente().getProfesional().getId())) {
            rechazar(turnoId, "el cliente no pertenece al profesional del turno");
        }
        String estadoCliente = gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.CLIENTE, turno.getCliente().getId());
        if (!ESTADOS_CLIENTE_VALIDOS.contains(estadoCliente)) {
            rechazar(turnoId, "el cliente tiene estado " + estadoCliente);
        }
        if (turno.getTipoAtencion() != null) {
            if (!turno.getTipoAtencion().isActivo()) {
                rechazar(turnoId, "el tipo de atencion esta inactivo");
            }
            if (!profesionalActual.equals(turno.getTipoAtencion().getProfesional().getId())) {
                rechazar(turnoId, "el tipo de atencion pertenece a otro profesional");
            }
        }

        LocalDate fechaInicio = nuevoInicio.atZone(clock.getZone()).toLocalDate();
        LocalDate fechaFin = nuevoFin.atZone(clock.getZone()).toLocalDate();
        if (!nuevoDia.getFecha().equals(fechaInicio) || !nuevoDia.getFecha().equals(fechaFin)) {
            rechazar(turnoId, "el horario no pertenece por completo al dia destino");
        }

        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, nuevoDia.getId());
        // Los dias creados por ConfigurarMesAgenda en el modelo legado no reciben
        // CambioEstado inicial. Una agenda configurada se interpreta como ACTIVO.
        if (estadoDia == null) {
            estadoDia = "ACTIVO";
        }
        if (!ESTADOS_DIA_VALIDOS.contains(estadoDia)) {
            rechazar(turnoId, "el dia destino tiene estado " + estadoDia);
        }
        if ("EN_TRANSCURSO".equals(estadoDia) && !nuevoInicio.isAfter(clock.instant())) {
            rechazar(turnoId, "el horario destino ya comenzo");
        }

        LocalTime horaInicio = nuevoInicio.atZone(clock.getZone()).toLocalTime();
        LocalTime horaFin = nuevoFin.atZone(clock.getZone()).toLocalTime();
        IntervaloHorario intervalo = new IntervaloHorario(horaInicio, horaFin);
        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(nuevoDia.getId());
        List<ExcepcionAgenda> excepciones = excepcionAgendaRepository
                .findActivasAplicablesAFecha(profesionalDestino, nuevoDia.getFecha());
        if (!evaluadorDisponibilidad.evaluar(
                nuevoDia, estadoDia, intervalo, brechas, excepciones).isEmpty()) {
            rechazar(turnoId, "el horario esta fuera de la disponibilidad efectiva");
        }

        boolean capacidadSuperada;
        if (turno.getTipoAtencion() != null) {
            capacidadSuperada = verificadorCapacidadTipoAtencion.evaluar(
                    turno.getTipoAtencion(), nuevoInicio, nuevoFin, turnoId).sobrecapacidad();
        } else {
            Configuracion configuracion = configuracionRepository
                    .findByProfesionalId(profesionalDestino)
                    .orElse(null);
            int maxSimultaneos = configuracion != null
                    ? configuracion.getCantidadMaxTurnosALaVez()
                    : 1;
            capacidadSuperada = verificadorCapacidad.excedidaCapacidad(
                    nuevoDia.getId(), nuevoInicio, nuevoFin, turnoId, maxSimultaneos);
        }
        if (capacidadSuperada) {
            rechazar(turnoId, "la capacidad del horario destino esta agotada");
        }
    }

    private void rechazar(Long turnoId, String detalle) {
        throw new ReprogramacionTurnoInvalidaException(turnoId, detalle);
    }
}
