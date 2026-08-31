package com.apiturnos.disponibilidad.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.turno.model.Turno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Determina si un Turno queda afectado (debe darse de baja) por una Excepción de Agenda.
 * <p>
 * Mientras el Profesional no cuente con zona horaria propia, se utiliza la zona horaria
 * de aplicación configurada (por defecto {@code America/Argentina/Buenos_Aires}).
 * </p>
 */
@Component
public class DetectorTurnoAfectadoPorExcepcion {

    private final ZoneId zonaHoraria;

    public DetectorTurnoAfectadoPorExcepcion(
            @Value("${turnos.zona-horaria:America/Argentina/Buenos_Aires}") String zonaHoraria) {
        this.zonaHoraria = ZoneId.of(zonaHoraria);
    }

    public boolean quedaAfectado(Turno turno,
                                 ExcepcionAgenda excepcionAplicada,
                                 List<IntervaloHorario> disponibilidadEfectiva) {
        TipoExcepcion tipo = excepcionAplicada.getTipo();
        if (tipo.esCierreDiaCompleto()
                || (tipo == TipoExcepcion.EXCEPCION_HORARIA
                    && excepcionAplicada.obtenerIntervalos().isEmpty())) {
            return true;
        }
        if (tipo == TipoExcepcion.HABILITACION_EXTRAORDINARIA) {
            return false;
        }

        IntervaloHorario intervaloTurno = convertirIntervalo(turno);
        if (intervaloTurno == null) {
            return true;
        }

        if (tipo.esBloqueoHorario()) {
            List<IntervaloHorario> bloqueos = excepcionAplicada.obtenerIntervalos();
            return bloqueos.stream().anyMatch(intervaloTurno::seSolapaCon);
        }

        return disponibilidadEfectiva.stream()
                .noneMatch(intervalo -> intervalo.contiene(intervaloTurno));
    }

    private IntervaloHorario convertirIntervalo(Turno turno) {
        ZonedDateTime inicio = turno.getInicioEstimado().atZone(zonaHoraria);
        ZonedDateTime fin = turno.getFinEstimado().atZone(zonaHoraria);
        LocalDate fechaDia = turno.getDiaAgenda().getFecha();

        if (!inicio.toLocalDate().equals(fechaDia)
                || !fin.toLocalDate().equals(fechaDia)) {
            return null;
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();
        if (!horaInicio.isBefore(horaFin)) {
            return null;
        }
        return new IntervaloHorario(horaInicio, horaFin);
    }
}
