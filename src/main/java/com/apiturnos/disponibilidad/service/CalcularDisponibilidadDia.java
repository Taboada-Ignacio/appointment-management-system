package com.apiturnos.disponibilidad.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Service
public class CalcularDisponibilidadDia {

    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final CalculadorDisponibilidadEfectiva calculador;

    public CalcularDisponibilidadDia(DiaAgendaRepository diaAgendaRepository,
                                     BrechaHorariaRepository brechaHorariaRepository,
                                     ExcepcionAgendaRepository excepcionAgendaRepository,
                                     GestorCambioEstado gestorCambioEstado,
                                     CalculadorDisponibilidadEfectiva calculador) {
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.calculador = calculador;
    }

    @Transactional(readOnly = true)
    public List<IntervaloHorario> ejecutar(Long profesionalId, LocalDate fecha) {
        DiaAgenda dia = diaAgendaRepository.findByProfesionalIdAndFecha(profesionalId, fecha)
                .orElseThrow(() -> new EntidadNoEncontradaException(
                        "DiaAgenda del profesional " + profesionalId + " para la fecha " + fecha));

        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
        List<ExcepcionAgenda> excepciones = excepcionAgendaRepository
                .findActivasAplicablesAFecha(profesionalId, fecha);
        String estadoDia = gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.DIA_AGENDA, dia.getId());

        return calcular(dia, estadoDia, brechas, excepciones);
    }

    /**
     * Calcula la disponibilidad efectiva para un día considerando la política de estados:
     * <ul>
     *   <li><b>FINALIZADO:</b> No admite disponibilidad ni reapertura silenciosa; retorna lista vacía.</li>
     *   <li><b>INACTIVO:</b> Base vacía, pero puede recibir {@code HABILITACION_EXTRAORDINARIA}.</li>
     *   <li><b>ACTIVO / EN_TRANSCURSO / null (compatibilidad):</b> Utiliza las brechas base configuradas.</li>
     * </ul>
     */
    public List<IntervaloHorario> calcular(DiaAgenda dia,
                                           String estadoDia,
                                           Collection<BrechaHoraria> brechasBase,
                                           Collection<ExcepcionAgenda> excepciones) {
        if ("FINALIZADO".equals(estadoDia)) {
            return List.of();
        }

        List<IntervaloHorario> base = esDiaBaseDisponible(estadoDia)
                ? brechasBase.stream()
                    .map(brecha -> new IntervaloHorario(
                            brecha.getHoraInicioAtencion(), brecha.getHoraFinAtencion()))
                    .toList()
                : List.of();

        List<ExcepcionAgenda> aplicables = excepciones.stream()
                .filter(ExcepcionAgenda::isActiva)
                .filter(excepcion -> excepcion.aplicaEn(dia.getFecha()))
                .toList();

        return calculador.calcular(base, aplicables);
    }

    private boolean esDiaBaseDisponible(String estadoDia) {
        return "ACTIVO".equals(estadoDia) || "EN_TRANSCURSO".equals(estadoDia) || estadoDia == null;
    }
}
