package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.ImpactoExcepcionAgendaResponseDto;
import com.apiturnos.agenda.dto.TurnoAfectadoExcepcionResponseDto;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.turno.model.Turno;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ConstructorImpactoExcepcionAgenda {

    private final GestorCambioEstado gestorCambioEstado;

    public ConstructorImpactoExcepcionAgenda(GestorCambioEstado gestorCambioEstado) {
        this.gestorCambioEstado = gestorCambioEstado;
    }

    public ImpactoExcepcionAgendaResponseDto construir(List<Turno> turnos) {
        return construir(null, turnos);
    }

    public ImpactoExcepcionAgendaResponseDto construir(String previewToken, List<Turno> turnos) {
        Map<Long, String> estados = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.TURNO, turnos.stream().map(Turno::getId).toList());
        List<TurnoAfectadoExcepcionResponseDto> detalles = turnos.stream()
                .map(turno -> convertir(turno, estados.get(turno.getId())))
                .toList();
        int notificables = (int) turnos.stream()
                .map(Turno::getCliente)
                .filter(cliente -> Boolean.TRUE.equals(cliente.getNotificacionesHabilitadas()))
                .count();
        return new ImpactoExcepcionAgendaResponseDto(
                previewToken, detalles.size(), notificables, detalles.size() - notificables, detalles);
    }

    private TurnoAfectadoExcepcionResponseDto convertir(Turno turno, String estado) {
        Cliente cliente = turno.getCliente();
        return new TurnoAfectadoExcepcionResponseDto(
                turno.getId(),
                turno.getDiaAgenda().getFecha(),
                turno.getInicioEstimado(),
                turno.getFinEstimado(),
                estado,
                cliente.getId(),
                cliente.getNombre() + " " + cliente.getApellido(),
                cliente.getTelefono(),
                Boolean.TRUE.equals(cliente.getNotificacionesHabilitadas()));
    }
}
