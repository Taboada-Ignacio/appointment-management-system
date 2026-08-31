package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.DiaAgendaNoValidoException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.MotivoRechazoTurnoManual;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ValidadorCrearTurnoManual {

    private static final Set<String> ESTADOS_CLIENTE_PERMITIDOS = Set.of(
            "HABILITADO", "REQUIERE_APROBACION");

    private final DiaAgendaRepository diaAgendaRepository;
    private final ClienteRepository clienteRepository;
    private final TipoAtencionRepository tipoAtencionRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad;
    private final VerificarCapacidadTipoAtencion verificadorCapacidad;
    private final Clock clock;

    public ValidadorCrearTurnoManual(
            DiaAgendaRepository diaAgendaRepository,
            ClienteRepository clienteRepository,
            TipoAtencionRepository tipoAtencionRepository,
            BrechaHorariaRepository brechaHorariaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            GestorCambioEstado gestorCambioEstado,
            EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad,
            VerificarCapacidadTipoAtencion verificadorCapacidad,
            Clock clock) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.clienteRepository = clienteRepository;
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.evaluadorDisponibilidad = evaluadorDisponibilidad;
        this.verificadorCapacidad = verificadorCapacidad;
        this.clock = clock;
    }

    public ContextoValidado validar(SolicitudCrearTurnoManual solicitud) {
        validarDatosObligatorios(solicitud);

        DiaAgenda dia = diaAgendaRepository
                .findByIdAndProfesionalId(solicitud.diaAgendaId(), solicitud.profesionalId())
                .orElseThrow(() -> new DiaAgendaNoValidoException(
                        "El día de agenda no existe o no pertenece al profesional"));

        Cliente cliente = clienteRepository
                .findByIdAndProfesionalId(solicitud.clienteId(), solicitud.profesionalId())
                .orElseThrow(() -> new ClienteNoPerteneceProfesionalException(
                        solicitud.clienteId(), solicitud.profesionalId()));

        TipoAtencion tipoAtencion = tipoAtencionRepository
                .findByIdAndProfesionalId(solicitud.tipoAtencionId(), solicitud.profesionalId())
                .orElseThrow(() -> new TipoAtencionNoPerteneceProfesionalException(
                        solicitud.tipoAtencionId(), solicitud.profesionalId()));

        validarCliente(cliente);
        validarTipoAtencion(tipoAtencion);

        LocalDate fechaInicio = solicitud.inicioEstimado().atZone(clock.getZone()).toLocalDate();
        LocalDate fechaFin = solicitud.finEstimado().atZone(clock.getZone()).toLocalDate();
        LocalTime horaInicio = solicitud.inicioEstimado().atZone(clock.getZone()).toLocalTime();
        LocalTime horaFin = solicitud.finEstimado().atZone(clock.getZone()).toLocalTime();

        String estadoDia = validarFechaYEstadoDia(
                dia, fechaInicio, fechaFin, solicitud.inicioEstimado());
        IntervaloHorario intervalo = new IntervaloHorario(horaInicio, horaFin);
        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
        List<ExcepcionAgenda> excepciones = excepcionAgendaRepository
                .findActivasAplicablesAFecha(solicitud.profesionalId(), dia.getFecha());

        List<AdvertenciaTurnoManual> advertencias = new ArrayList<>(
                evaluadorDisponibilidad.evaluar(dia, estadoDia, intervalo, brechas, excepciones));

        VerificarCapacidadTipoAtencion.ResultadoCapacidad capacidad =
                verificadorCapacidad.evaluar(
                        tipoAtencion,
                        solicitud.inicioEstimado(),
                        solicitud.finEstimado(),
                        null);
        if (capacidad.sobrecapacidad()) {
            advertencias.add(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA);
        }

        DatosConfirmacionTurnoManual datos = new DatosConfirmacionTurnoManual(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                dia.getFecha(),
                horaInicio,
                horaFin,
                tipoAtencion.getId(),
                tipoAtencion.getNombre());

        return new ContextoValidado(dia, cliente, tipoAtencion, List.copyOf(advertencias), datos);
    }

    private void validarDatosObligatorios(SolicitudCrearTurnoManual solicitud) {
        if (solicitud == null
                || solicitud.profesionalId() == null
                || solicitud.diaAgendaId() == null
                || solicitud.clienteId() == null
                || solicitud.tipoAtencionId() == null
                || solicitud.inicioEstimado() == null
                || solicitud.finEstimado() == null
                || solicitud.usuario() == null
                || solicitud.usuario().isBlank()) {
            rechazar(MotivoRechazoTurnoManual.DATOS_INVALIDOS,
                    "Profesional, día, cliente, tipo de atención, horario y usuario son obligatorios");
        }
        if (!solicitud.inicioEstimado().isBefore(solicitud.finEstimado())) {
            rechazar(MotivoRechazoTurnoManual.DATOS_INVALIDOS,
                    "El inicio estimado debe ser anterior al fin estimado");
        }
    }

    private void validarCliente(Cliente cliente) {
        String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId());
        if (estado == null || !ESTADOS_CLIENTE_PERMITIDOS.contains(estado)) {
            rechazar(MotivoRechazoTurnoManual.CLIENTE_NO_HABILITADO,
                    "El cliente tiene estado '" + estado + "' y no admite alta manual de turnos");
        }
    }

    private void validarTipoAtencion(TipoAtencion tipoAtencion) {
        if (!tipoAtencion.isActivo()) {
            rechazar(MotivoRechazoTurnoManual.TIPO_ATENCION_INACTIVO,
                    "El tipo de atención está inactivo");
        }
    }

    private String validarFechaYEstadoDia(
            DiaAgenda dia,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Instant inicioEstimado) {

        if (!dia.getFecha().equals(fechaInicio) || !dia.getFecha().equals(fechaFin)) {
            rechazar(MotivoRechazoTurnoManual.HORARIO_NO_PERTENECE_AL_DIA,
                    "El horario completo debe pertenecer a la fecha del día de agenda");
        }
        if (dia.getFecha().isBefore(LocalDate.now(clock))) {
            rechazar(MotivoRechazoTurnoManual.FECHA_PASADA,
                    "No se pueden crear turnos manuales en fechas anteriores");
        }

        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, dia.getId());
        if ("INACTIVO".equals(estadoDia)) {
            rechazar(MotivoRechazoTurnoManual.DIA_INACTIVO,
                    "Un día de agenda inactivo no admite turnos manuales");
        }
        if ("FINALIZADO".equals(estadoDia)) {
            rechazar(MotivoRechazoTurnoManual.DIA_FINALIZADO,
                    "Un día de agenda finalizado no admite turnos manuales");
        }
        if (!"ACTIVO".equals(estadoDia) && !"EN_TRANSCURSO".equals(estadoDia)) {
            rechazar(MotivoRechazoTurnoManual.ESTADO_DIA_NO_PERMITIDO,
                    "El día de agenda no tiene un estado seleccionable");
        }
        if ("EN_TRANSCURSO".equals(estadoDia) && !inicioEstimado.isAfter(clock.instant())) {
            rechazar(MotivoRechazoTurnoManual.HORARIO_YA_INICIADO,
                    "En un día en curso sólo se permiten horarios que todavía no comenzaron");
        }
        return estadoDia;
    }

    private void rechazar(MotivoRechazoTurnoManual motivo, String mensaje) {
        throw new TurnoManualNoPermitidoException(motivo, mensaje);
    }

    public record ContextoValidado(
            DiaAgenda diaAgenda,
            Cliente cliente,
            TipoAtencion tipoAtencion,
            List<AdvertenciaTurnoManual> advertencias,
            DatosConfirmacionTurnoManual datosConfirmacion) {
    }
}
