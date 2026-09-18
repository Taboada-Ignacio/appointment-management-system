package com.apiturnos.turno.service;

import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.repository.TurnoRepository;
import java.util.Set;

final class ReglaTurnosClienteDia {
    private ReglaTurnosClienteDia() {}
    static void validar(Configuracion config, Long dia, Long cliente,
                        TurnoRepository turnos, GestorCambioEstado estados) {
        if (config != null && Boolean.TRUE.equals(config.getPermitirMultiplesTurnosPorClienteEnDia())) return;
        for (var turno : turnos.findByDiaAgendaId(dia)) {
            if (!turno.getCliente().getId().equals(cliente)) continue;
            String estado = estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turno.getId());
            if (!Set.of("CANCELADO", "DADO_DE_BAJA", "RECHAZADO").contains(estado == null ? "" : estado))
                throw new NegocioException("El cliente ya tiene un turno en este día. El profesional no permite más de un turno por cliente en el mismo día.");
        }
    }
}
