package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.DiaSeleccionableResponseDto;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.service.EvaluadorDisponibilidadTurnoManual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ObtenerDiasSeleccionables {

    private final DiaAgendaRepository diaAgendaRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad;
    private final ProfesionalRepository profesionalRepository;
    private final Clock clock;

    public ObtenerDiasSeleccionables(
            DiaAgendaRepository diaAgendaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            GestorCambioEstado gestorCambioEstado,
            EvaluadorDisponibilidadTurnoManual evaluadorDisponibilidad,
            ProfesionalRepository profesionalRepository,
            Clock clock) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.evaluadorDisponibilidad = evaluadorDisponibilidad;
        this.profesionalRepository = profesionalRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DiaSeleccionableResponseDto> ejecutar(Long profesionalId, LocalDate desde, LocalDate hasta) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }

        if (!profesionalRepository.existsById(profesionalId)) {
            throw new EntidadNoEncontradaException("Profesional", profesionalId);
        }

        LocalDate hoy = LocalDate.now(clock);
        LocalDate fechaDesde = desde != null ? desde : hoy;
        LocalDate fechaHasta = hasta != null ? hasta : fechaDesde.plusMonths(1);

        if (fechaHasta.isBefore(fechaDesde)) {
            throw new NegocioException("La fecha 'hasta' no puede ser anterior a la fecha 'desde'");
        }

        List<DiaAgenda> dias = diaAgendaRepository.findByProfesionalIdAndFechaBetween(
                profesionalId, fechaDesde, fechaHasta);

        List<DiaSeleccionableResponseDto> resultado = new ArrayList<>();

        for (DiaAgenda dia : dias) {
            String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(
                    AmbitoEstado.DIA_AGENDA, dia.getId());
            String nombreDiaSemana = dia.getFecha().getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));

            boolean seleccionable;
            String mensaje = null;

            if (dia.getFecha().isBefore(hoy)) {
                seleccionable = false;
                mensaje = "Fecha anterior no seleccionable";
            } else {
                List<ExcepcionAgenda> excepciones = excepcionAgendaRepository
                        .findActivasAplicablesAFecha(profesionalId, dia.getFecha());

                if (evaluadorDisponibilidad.hayCierreCompleto(excepciones)) {
                    seleccionable = false;
                    mensaje = "Día cerrado por excepción de agenda";
                } else if ("ACTIVO".equals(estadoDia)) {
                    seleccionable = true;
                } else if ("EN_TRANSCURSO".equals(estadoDia)) {
                    seleccionable = true;
                    mensaje = "Seleccionable para horarios futuros";
                } else if ("INACTIVO".equals(estadoDia)) {
                    seleccionable = false;
                    mensaje = "Día inactivo";
                } else if ("FINALIZADO".equals(estadoDia)) {
                    seleccionable = false;
                    mensaje = "Día finalizado";
                } else {
                    seleccionable = false;
                    mensaje = "Estado de día no seleccionable";
                }
            }

            resultado.add(new DiaSeleccionableResponseDto(
                    dia.getId(),
                    dia.getFecha(),
                    estadoDia,
                    nombreDiaSemana,
                    seleccionable,
                    mensaje));
        }

        resultado.sort(Comparator.comparing(DiaSeleccionableResponseDto::getFecha));
        return List.copyOf(resultado);
    }
}

