package com.apiturnos.atencion.service;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.CapacidadAgotadaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class VerificarCapacidadTipoAtencion {

    private static final Set<String> ESTADOS_ACTIVOS = Set.of(
            "ASIGNADO", "PENDIENTE_DE_APROBACION", "CONFIRMADO", "REPROGRAMADO", "AFECTADO_POR_EXCEPCION");

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
        int count = 0;
        for (Turno t : solapados) {
            if (excluirTurnoId != null && excluirTurnoId.equals(t.getId())) {
                continue;
            }
            String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, t.getId());
            if (estado != null && ESTADOS_ACTIVOS.contains(estado)) {
                count++;
            }
        }
        return count;
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
}

