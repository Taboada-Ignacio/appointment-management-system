package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SugerirHorariosTurnoManual {

    private final TipoAtencionRepository tipoAtencionRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final CalcularDisponibilidadDia calcularDisponibilidadDia;
    private final VerificarCapacidadTipoAtencion verificadorCapacidad;
    private final GestorCambioEstado gestorCambioEstado;
    private final EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad;
    private final Clock clock;

    public SugerirHorariosTurnoManual(
            TipoAtencionRepository tipoAtencionRepository,
            DiaAgendaRepository diaAgendaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            CalcularDisponibilidadDia calcularDisponibilidadDia,
            VerificarCapacidadTipoAtencion verificadorCapacidad,
            GestorCambioEstado gestorCambioEstado,
            EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad,
            Clock clock) {
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.calcularDisponibilidadDia = calcularDisponibilidadDia;
        this.verificadorCapacidad = verificadorCapacidad;
        this.gestorCambioEstado = gestorCambioEstado;
        this.evaluadorDisponibilidad = evaluadorDisponibilidad;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<HorarioSugeridoTurnoManual> ejecutar(
            Long profesionalId,
            Long tipoAtencionId,
            LocalDate fecha) {
        if (profesionalId == null || tipoAtencionId == null || fecha == null) {
            throw new NegocioException("Profesional, tipo de atención y fecha son obligatorios");
        }

        TipoAtencion tipo = tipoAtencionRepository
                .findByIdAndProfesionalId(tipoAtencionId, profesionalId)
                .orElseThrow(() -> new TipoAtencionNoPerteneceProfesionalException(
                        tipoAtencionId, profesionalId));
        if (!tipo.isActivo()) {
            return List.of();
        }

        DiaAgenda dia = diaAgendaRepository.findByProfesionalIdAndFecha(profesionalId, fecha)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "DiaAgenda del profesional " + profesionalId + " para la fecha " + fecha));
        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.DIA_AGENDA, dia.getId());
        if (fecha.isBefore(LocalDate.now(clock))
                || (!"ACTIVO".equals(estadoDia) && !"EN_TRANSCURSO".equals(estadoDia))) {
            return List.of();
        }

        List<ExcepcionAgenda> excepciones = excepcionAgendaRepository
                .findActivasAplicablesAFecha(profesionalId, fecha);
        if (evaluadorDisponibilidad.hayCierreCompleto(excepciones)) {
            return List.of();
        }

        List<IntervaloHorario> efectivos = calcularDisponibilidadDia.ejecutar(profesionalId, fecha);
        int duracion = tipo.getDuracionMinutos();
        if (duracion <= 0) {
            throw new NegocioException("La duración del tipo de atención debe ser mayor que cero");
        }
        List<HorarioSugeridoTurnoManual> sugerencias = new ArrayList<>();

        for (IntervaloHorario franja : efectivos) {
            LocalTime inicio = franja.inicio();
            while (!inicio.plusMinutes(duracion).isAfter(franja.fin())) {
                LocalTime fin = inicio.plusMinutes(duracion);
                IntervaloHorario intervalo = new IntervaloHorario(inicio, fin);

                if (!evaluadorDisponibilidad.intersectaBloqueoExplicito(intervalo, excepciones)) {
                    Instant inicioInstant = fecha.atTime(inicio).atZone(clock.getZone()).toInstant();
                    Instant finInstant = fecha.atTime(fin).atZone(clock.getZone()).toInstant();

                    if (!"EN_TRANSCURSO".equals(estadoDia) || inicioInstant.isAfter(clock.instant())) {
                        VerificarCapacidadTipoAtencion.ResultadoCapacidad capacidad =
                                verificadorCapacidad.evaluar(tipo, inicioInstant, finInstant, null);
                        List<AdvertenciaTurnoManual> advertencias = capacidad.sobrecapacidad()
                                ? List.of(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA)
                                : List.of();
                        sugerencias.add(new HorarioSugeridoTurnoManual(
                                inicio,
                                fin,
                                duracion,
                                capacidad.turnosConcurrentes(),
                                capacidad.capacidadMaxima(),
                                advertencias));
                    }
                }

                inicio = fin;
            }
        }

        return List.copyOf(sugerencias);
    }
}
