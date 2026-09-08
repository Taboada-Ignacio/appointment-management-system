package com.apiturnos.atencion.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.CapacidadAgotadaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;

@Service
public class VerificarCapacidadTipoAtencion {

    private static final String ESTADO_QUE_OCUPA_CAPACIDAD = "ASIGNADO";

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public VerificarCapacidadTipoAtencion(TurnoRepository turnoRepository,
                                          GestorCambioEstado gestorCambioEstado) {
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    public record ResultadoCapacidad(
            int turnosConcurrentes,
            int capacidadMaxima,
            boolean disponible,
            boolean sobrecapacidad) {
    }

    public int contarTurnosConcurrentes(Long tipoAtencionId, Instant inicio, Instant fin, Long excluirTurnoId) {
        Objects.requireNonNull(tipoAtencionId, "El ID de TipoAtencion es obligatorio");
        Objects.requireNonNull(inicio, "El inicio estimado es obligatorio");
        Objects.requireNonNull(fin, "El fin estimado es obligatorio");

        List<Turno> solapados = turnoRepository.findTurnosSolapadosPorTipoAtencion(tipoAtencionId, inicio, fin);
        List<EventoConcurrencia> eventos = new ArrayList<>();
        for (Turno t : solapados) {
            if (excluirTurnoId != null && excluirTurnoId.equals(t.getId())) {
                continue;
            }
            String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, t.getId());
            if (ESTADO_QUE_OCUPA_CAPACIDAD.equals(estado)) {
                Instant desde = t.getInicioEstimado().isBefore(inicio) ? inicio : t.getInicioEstimado();
                Instant hasta = t.getFinEstimado().isAfter(fin) ? fin : t.getFinEstimado();
                if (desde.isBefore(hasta)) {
                    eventos.add(new EventoConcurrencia(desde, 1));
                    eventos.add(new EventoConcurrencia(hasta, -1));
                }
            }
        }
        eventos.sort(Comparator.comparing(EventoConcurrencia::instante)
                .thenComparingInt(EventoConcurrencia::delta)); // los finales preceden a los inicios contiguos
        int actuales = 0;
        int maximo = 0;
        for (EventoConcurrencia evento : eventos) {
            actuales += evento.delta();
            maximo = Math.max(maximo, actuales);
        }
        return maximo;
    }

    public ResultadoCapacidad evaluar(TipoAtencion tipoAtencion, Instant inicio, Instant fin, Long excluirTurnoId) {
        Objects.requireNonNull(tipoAtencion, "El TipoAtencion es obligatorio");
        int capacidadMax = tipoAtencion.getCapacidadSimultanea() != null ? tipoAtencion.getCapacidadSimultanea() : 1;
        int concurrentes = contarTurnosConcurrentes(tipoAtencion.getId(), inicio, fin, excluirTurnoId);
        boolean disponible = concurrentes < capacidadMax;
        boolean sobrecapacidad = concurrentes >= capacidadMax;
        return new ResultadoCapacidad(concurrentes, capacidadMax, disponible, sobrecapacidad);
    }

    public void verificarCapacidadAutoGestion(TipoAtencion tipoAtencion, Instant inicio, Instant fin) {
        ResultadoCapacidad resultado = evaluar(tipoAtencion, inicio, fin, null);
        if (resultado.sobrecapacidad()) {
            throw new CapacidadAgotadaException(resultado.turnosConcurrentes(), resultado.capacidadMaxima());
        }
    }

    public boolean esSobrecapacidadManual(TipoAtencion tipoAtencion, Instant inicio, Instant fin) {
        ResultadoCapacidad resultado = evaluar(tipoAtencion, inicio, fin, null);
        return resultado.sobrecapacidad();
    }

    public ResultadoCapacidad evaluarConfiguracion(Long profesionalId, LocalDate fecha,
                                                    int capacidadMax, Instant inicio, Instant fin,
                                                    Long excluirTurnoId) {
        Objects.requireNonNull(profesionalId, "El profesional es obligatorio");
        List<Turno> solapados = turnoRepository.findIntersectandoFranja(profesionalId, fecha, inicio, fin);
        int concurrentes = contarConcurrencia(solapados, inicio, fin, excluirTurnoId);
        return new ResultadoCapacidad(concurrentes, capacidadMax,
                concurrentes < capacidadMax, concurrentes >= capacidadMax);
    }

    private int contarConcurrencia(List<Turno> solapados, Instant inicio, Instant fin, Long excluirTurnoId) {
        List<EventoConcurrencia> eventos = new ArrayList<>();
        for (Turno t : solapados) {
            if (excluirTurnoId != null && excluirTurnoId.equals(t.getId())) continue;
            String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, t.getId());
            if (ESTADO_QUE_OCUPA_CAPACIDAD.equals(estado)) {
                Instant desde = t.getInicioEstimado().isBefore(inicio) ? inicio : t.getInicioEstimado();
                Instant hasta = t.getFinEstimado().isAfter(fin) ? fin : t.getFinEstimado();
                if (desde.isBefore(hasta)) {
                    eventos.add(new EventoConcurrencia(desde, 1));
                    eventos.add(new EventoConcurrencia(hasta, -1));
                }
            }
        }
        eventos.sort(Comparator.comparing(EventoConcurrencia::instante).thenComparingInt(EventoConcurrencia::delta));
        int actuales = 0, maximo = 0;
        for (EventoConcurrencia evento : eventos) { actuales += evento.delta(); maximo = Math.max(maximo, actuales); }
        return maximo;
    }

    private record EventoConcurrencia(Instant instante, int delta) {
    }
}

