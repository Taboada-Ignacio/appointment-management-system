package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.model.MotivoRechazoTurnoManual;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class EvaluadorDisponibilidadTurnoManual {

    private final CalcularDisponibilidadDia calcularDisponibilidadDia;

    public EvaluadorDisponibilidadTurnoManual(CalcularDisponibilidadDia calcularDisponibilidadDia) {
        this.calcularDisponibilidadDia = calcularDisponibilidadDia;
    }

    public List<AdvertenciaTurnoManual> evaluar(
            DiaAgenda dia,
            String estadoDia,
            IntervaloHorario intervaloSolicitado,
            Collection<BrechaHoraria> brechasBase,
            Collection<ExcepcionAgenda> excepciones) {

        List<ExcepcionAgenda> aplicables = excepciones.stream()
                .filter(ExcepcionAgenda::isActiva)
                .filter(excepcion -> !dia.getFecha().isBefore(excepcion.getFechaInicio()))
                .filter(excepcion -> !dia.getFecha().isAfter(excepcion.getFechaFin()))
                .toList();

        if (hayCierreCompleto(aplicables)) {
            throw new TurnoManualNoPermitidoException(
                    MotivoRechazoTurnoManual.DIA_CERRADO_POR_EXCEPCION,
                    "La fecha está cerrada por una excepción de agenda y no admite turnos manuales");
        }

        if (intersectaBloqueoExplicito(intervaloSolicitado, aplicables)) {
            throw new TurnoManualNoPermitidoException(
                    MotivoRechazoTurnoManual.HORARIO_BLOQUEADO_POR_EXCEPCION,
                    "El horario solicitado intersecta un bloqueo explícito de agenda");
        }

        List<IntervaloHorario> efectivos = calcularDisponibilidadDia.calcular(
                dia, estadoDia, brechasBase, aplicables);
        boolean contenido = efectivos.stream().anyMatch(intervalo -> intervalo.contiene(intervaloSolicitado));

        return contenido
                ? List.of()
                : List.of(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA);
    }

    public boolean hayCierreCompleto(Collection<ExcepcionAgenda> excepciones) {
        return excepciones.stream()
                .filter(ExcepcionAgenda::isActiva)
                .anyMatch(this::esCierreCompleto);
    }

    public boolean intersectaBloqueoExplicito(
            IntervaloHorario intervalo,
            Collection<ExcepcionAgenda> excepciones) {
        return excepciones.stream()
                .filter(ExcepcionAgenda::isActiva)
                .filter(excepcion -> excepcion.getTipo().esBloqueoHorario())
                .flatMap(excepcion -> excepcion.obtenerIntervalos().stream())
                .anyMatch(intervalo::seSolapaCon);
    }

    private boolean esCierreCompleto(ExcepcionAgenda excepcion) {
        if (excepcion.getTipo().esCierreDiaCompleto()) {
            return true;
        }
        return excepcion.getTipo() == TipoExcepcion.EXCEPCION_HORARIA
                && excepcion.obtenerIntervalos().isEmpty();
    }
}
