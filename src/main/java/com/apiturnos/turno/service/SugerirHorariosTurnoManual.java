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
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ConfiguracionRepository configuracionRepository;

    public SugerirHorariosTurnoManual(
            TipoAtencionRepository tipoAtencionRepository,
            DiaAgendaRepository diaAgendaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            CalcularDisponibilidadDia calcularDisponibilidadDia,
            VerificarCapacidadTipoAtencion verificadorCapacidad,
            GestorCambioEstado gestorCambioEstado,
            EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad,
            Clock clock) {
        this(tipoAtencionRepository, diaAgendaRepository, excepcionAgendaRepository,
                calcularDisponibilidadDia, verificadorCapacidad, gestorCambioEstado,
                evaluadorDisponibilidad, clock, null);
    }

    @Autowired
    public SugerirHorariosTurnoManual(
            TipoAtencionRepository tipoAtencionRepository, DiaAgendaRepository diaAgendaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository, CalcularDisponibilidadDia calcularDisponibilidadDia,
            VerificarCapacidadTipoAtencion verificadorCapacidad, GestorCambioEstado gestorCambioEstado,
            EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad, Clock clock,
            ConfiguracionRepository configuracionRepository) {
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.calcularDisponibilidadDia = calcularDisponibilidadDia;
        this.verificadorCapacidad = verificadorCapacidad;
        this.gestorCambioEstado = gestorCambioEstado;
        this.evaluadorDisponibilidad = evaluadorDisponibilidad;
        this.clock = clock;
        this.configuracionRepository = configuracionRepository;
    }

    public List<HorarioSugeridoTurnoManual> ejecutar(Long profesionalId, LocalDate fecha) {
        if (configuracionRepository == null) throw new NegocioException("La configuración profesional es obligatoria");
        Configuracion configuracion = configuracionRepository.findByProfesionalId(profesionalId)
                .orElseThrow(() -> new NegocioException("El profesional no tiene configuración"));
        return generar(profesionalId, fecha, configuracion.getDuracionAproximadaPorTurno(),
                configuracion.getCantidadMaxTurnosALaVez(), null);
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

        return generar(profesionalId, fecha, tipo.getDuracionMinutos(), tipo.getCapacidadSimultanea(), tipo);
    }

    private List<HorarioSugeridoTurnoManual> generar(Long profesionalId, LocalDate fecha, int duracion,
                                                      int capacidadConfigurada, TipoAtencion tipo) {
        DiaAgenda dia = diaAgendaRepository.findByProfesionalIdAndFecha(profesionalId, fecha)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "DiaAgenda del profesional " + profesionalId + " para la fecha " + fecha));
        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, dia.getId());
        if (fecha.isBefore(LocalDate.now(clock)) || (!"ACTIVO".equals(estadoDia) && !"EN_TRANSCURSO".equals(estadoDia))) return List.of();
        List<ExcepcionAgenda> excepciones = excepcionAgendaRepository
                .findActivasAplicablesAFecha(profesionalId, fecha);
        if (evaluadorDisponibilidad.hayCierreCompleto(excepciones)) {
            return List.of();
        }

        List<IntervaloHorario> efectivos = calcularDisponibilidadDia.ejecutar(profesionalId, fecha);
        if (duracion <= 0) {
            throw new NegocioException("La duración del tipo de atención debe ser mayor que cero");
        }
        List<HorarioSugeridoTurnoManual> sugerencias = new ArrayList<>();

        for (IntervaloHorario franja : efectivos) {
            LocalTime inicio = franja.inicio();
            while (inicio.isBefore(franja.fin())) {
                LocalTime fin = inicio.plusMinutes(duracion);
                IntervaloHorario intervalo = new IntervaloHorario(inicio, fin);

                if (!evaluadorDisponibilidad.intersectaBloqueoExplicito(intervalo, excepciones)) {
                    Instant inicioInstant = fecha.atTime(inicio).atZone(clock.getZone()).toInstant();
                    Instant finInstant = fecha.atTime(fin).atZone(clock.getZone()).toInstant();

                    if (!"EN_TRANSCURSO".equals(estadoDia) || inicioInstant.isAfter(clock.instant())) {
                        VerificarCapacidadTipoAtencion.ResultadoCapacidad capacidad = tipo != null
                                ? verificadorCapacidad.evaluar(tipo, inicioInstant, finInstant, null)
                                : verificadorCapacidad.evaluarConfiguracion(profesionalId, fecha,
                                        capacidadConfigurada, inicioInstant, finInstant, null);
                        List<AdvertenciaTurnoManual> advertencias = new ArrayList<>();
                        if (fin.isAfter(franja.fin())) {
                            advertencias.add(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA);
                        }
                        if (capacidad.sobrecapacidad()) {
                            advertencias.add(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA);
                        }
                        sugerencias.add(new HorarioSugeridoTurnoManual(
                                inicio,
                                fin,
                                duracion,
                                capacidad.turnosConcurrentes(),
                                capacidad.capacidadMaxima(),
                                List.copyOf(advertencias)));
                    }
                }

                inicio = fin;
            }
        }

        return List.copyOf(sugerencias);
    }
}
