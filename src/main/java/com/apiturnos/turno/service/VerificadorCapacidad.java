package com.apiturnos.turno.service;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.CapacidadAgotadaException;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class VerificadorCapacidad {

    private static final Set<String> ESTADOS_ACTIVOS = Set.of(
            "ASIGNADO", "PENDIENTE_DE_APROBACION", "CONFIRMADO", "REPROGRAMADO", "AFECTADO_POR_EXCEPCION");

    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public VerificadorCapacidad(TurnoRepository turnoRepository, GestorCambioEstado gestorCambioEstado) {
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    public int contarTurnosActivosSolapados(Long diaAgendaId, Instant inicio, Instant fin) {
        List<Turno> turnosDia = turnoRepository.findByDiaAgendaId(diaAgendaId);
        int count = 0;
        for (Turno t : turnosDia) {
            if (seSolapan(t.getInicioEstimado(), t.getFinEstimado(), inicio, fin)) {
                String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, t.getId());
                if (estado != null && ESTADOS_ACTIVOS.contains(estado)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void verificarCapacidadAutoGestion(Long diaAgendaId, Instant inicio, Instant fin, int maxSimultaneos) {
        int solapados = contarTurnosActivosSolapados(diaAgendaId, inicio, fin);
        if (solapados >= maxSimultaneos) {
            throw new CapacidadAgotadaException(solapados, maxSimultaneos);
        }
    }

    public boolean excedidaCapacidadManual(Long diaAgendaId, Instant inicio, Instant fin, int maxSimultaneos) {
        int solapados = contarTurnosActivosSolapados(diaAgendaId, inicio, fin);
        return solapados >= maxSimultaneos;
    }

    public boolean excedidaCapacidad(Long diaAgendaId, Instant inicio, Instant fin,
                                     Long excluirTurnoId, int maxSimultaneos) {
        int solapados = 0;
        for (Turno turno : turnoRepository.findByDiaAgendaId(diaAgendaId)) {
            if (excluirTurnoId != null && excluirTurnoId.equals(turno.getId())) {
                continue;
            }
            if (seSolapan(turno.getInicioEstimado(), turno.getFinEstimado(), inicio, fin)) {
                String estado = gestorCambioEstado.obtenerNombreEstadoActual(
                        AmbitoEstado.TURNO, turno.getId());
                if (estado != null && ESTADOS_ACTIVOS.contains(estado)) {
                    solapados++;
                }
            }
        }
        return solapados >= maxSimultaneos;
    }

    private boolean seSolapan(Instant inicio1, Instant fin1, Instant inicio2, Instant fin2) {
        return inicio1.isBefore(fin2) && inicio2.isBefore(fin1);
    }
}
