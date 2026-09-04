package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.disponibilidad.service.DetectorTurnoAfectadoPorExcepcion;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EvaluarImpactoExcepcionAgenda {

    private static final Set<String> ESTADOS_AFECTABLES = Set.of(
            "ASIGNADO", "PENDIENTE_DE_APROBACION", "CONFIRMADO", "REPROGRAMADO");

    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final CalcularDisponibilidadDia calcularDisponibilidadDia;
    private final DetectorTurnoAfectadoPorExcepcion detectorTurnoAfectado;
    private final ProcesarBajasTurnosPorExcepcion procesarBajas;
    private final ZoneId zonaHoraria;

    public EvaluarImpactoExcepcionAgenda(
            DiaAgendaRepository diaAgendaRepository,
            BrechaHorariaRepository brechaHorariaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            TurnoRepository turnoRepository,
            GestorCambioEstado gestorCambioEstado,
            CalcularDisponibilidadDia calcularDisponibilidadDia,
            DetectorTurnoAfectadoPorExcepcion detectorTurnoAfectado,
            ProcesarBajasTurnosPorExcepcion procesarBajas,
            @Value("${turnos.zona-horaria:America/Argentina/Buenos_Aires}") String zonaHoraria) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.calcularDisponibilidadDia = calcularDisponibilidadDia;
        this.detectorTurnoAfectado = detectorTurnoAfectado;
        this.procesarBajas = procesarBajas;
        this.zonaHoraria = ZoneId.of(zonaHoraria);
    }

    public List<Turno> ejecutar(ExcepcionAgenda excepcion, String usuario) {
        List<Turno> afectados = previsualizar(excepcion);
        procesarBajas.ejecutar(excepcion, afectados, usuario);
        return afectados;
    }

    /**
     * Calcula los turnos que quedarían fuera de la disponibilidad efectiva sin
     * persistir estados, motivos, auditorías ni notificaciones.
     */
    public List<Turno> previsualizar(ExcepcionAgenda excepcion) {
        // Habilitación extraordinaria nunca da de baja turnos existentes
        if (excepcion.getTipo() == TipoExcepcion.HABILITACION_EXTRAORDINARIA) {
            return List.of();
        }

        Long profesionalId = excepcion.getProfesional().getId();
        if (excepcion.getFechaInicio().equals(excepcion.getFechaFin())
                && !excepcion.aplicaEn(excepcion.getFechaInicio())) {
            return List.of();
        }

        // Optimización para BLOQUEO_HORARIO de un único día: query directa de intersección
        if (excepcion.getTipo().esBloqueoHorario()
                && excepcion.getFechaInicio().equals(excepcion.getFechaFin())
                && !excepcion.obtenerIntervalos().isEmpty()) {
            return evaluarImpactoBloqueoDirecto(excepcion, profesionalId);
        }

        return evaluarImpactoPorDisponibilidadGeneral(excepcion, profesionalId);
    }

    private List<Turno> evaluarImpactoBloqueoDirecto(
            ExcepcionAgenda excepcion,
            Long profesionalId) {

        LocalDate fecha = excepcion.getFechaInicio();
        Set<Turno> turnosIntersectados = new LinkedHashSet<>();

        for (IntervaloHorario intervalo : excepcion.obtenerIntervalos()) {
            Instant inicioInstante = fecha.atTime(intervalo.inicio()).atZone(zonaHoraria).toInstant();
            Instant finInstante = fecha.atTime(intervalo.fin()).atZone(zonaHoraria).toInstant();

            List<Turno> encontrados = turnoRepository.findIntersectandoFranja(
                    profesionalId, fecha, inicioInstante, finInstante);
            turnosIntersectados.addAll(encontrados);
        }

        if (turnosIntersectados.isEmpty()) {
            return List.of();
        }

        List<Turno> listaTurnos = new ArrayList<>(turnosIntersectados);
        List<Long> turnoIds = listaTurnos.stream().map(Turno::getId).toList();
        Map<Long, String> estadosTurnos = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.TURNO, turnoIds);

        List<Turno> afectados = listaTurnos.stream()
                .filter(t -> ESTADOS_AFECTABLES.contains(estadosTurnos.get(t.getId())))
                .toList();

        return List.copyOf(afectados);
    }

    private List<Turno> evaluarImpactoPorDisponibilidadGeneral(
            ExcepcionAgenda excepcion,
            Long profesionalId) {

        List<DiaAgenda> dias = diaAgendaRepository.findByProfesionalIdAndFechaBetween(
                profesionalId, excepcion.getFechaInicio(), excepcion.getFechaFin());
        if (dias.isEmpty()) {
            return List.of();
        }

        List<Long> diaIds = dias.stream().map(DiaAgenda::getId).toList();
        Map<Long, List<BrechaHoraria>> brechasPorDia = brechaHorariaRepository
                .findByDiaAgendaIdInOrderByDiaAgendaIdAscHoraInicioAtencionAsc(diaIds)
                .stream()
                .collect(Collectors.groupingBy(brecha -> brecha.getDiaAgenda().getId()));
        Map<Long, String> estadosDias = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.DIA_AGENDA, diaIds);
        List<ExcepcionAgenda> excepcionesRangoPersistidas = excepcionAgendaRepository
                .findActivasIntersectandoRango(
                        profesionalId, excepcion.getFechaInicio(), excepcion.getFechaFin());
        List<ExcepcionAgenda> excepcionesRango = new ArrayList<>(excepcionesRangoPersistidas.stream()
                .filter(actual -> excepcion.getId() == null || !excepcion.getId().equals(actual.getId()))
                .toList());
        excepcionesRango.add(excepcion);

        Map<LocalDate, List<IntervaloHorario>> disponibilidadPorFecha = new HashMap<>();
        for (DiaAgenda dia : dias) {
            disponibilidadPorFecha.put(
                    dia.getFecha(),
                    calcularDisponibilidadDia.calcular(
                            dia,
                            estadosDias.get(dia.getId()),
                            brechasPorDia.getOrDefault(dia.getId(), List.of()),
                            excepcionesRango));
        }

        List<Turno> turnosRango = turnoRepository.findByProfesionalAndFechaBetween(
                profesionalId, excepcion.getFechaInicio(), excepcion.getFechaFin());
        if (turnosRango.isEmpty()) {
            return List.of();
        }

        List<Long> turnoIds = turnosRango.stream().map(Turno::getId).toList();
        Map<Long, String> estadosTurnos = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.TURNO, turnoIds);

        List<Turno> afectados = new ArrayList<>();
        for (Turno turno : turnosRango) {
            if (!excepcion.aplicaEn(turno.getDiaAgenda().getFecha())) {
                continue;
            }
            if (!ESTADOS_AFECTABLES.contains(estadosTurnos.get(turno.getId()))) {
                continue;
            }
            List<IntervaloHorario> disponibilidad = disponibilidadPorFecha.getOrDefault(
                    turno.getDiaAgenda().getFecha(), List.of());
            if (detectorTurnoAfectado.quedaAfectado(turno, excepcion, disponibilidad)) {
                afectados.add(turno);
            }
        }

        return List.copyOf(afectados);
    }
}
