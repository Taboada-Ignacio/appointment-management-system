package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.service.DarDeBajaTurno;
import com.apiturnos.turno.service.PoliticaTransicionesTurno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Aplica el mismo flujo de baja cuando una edición de agenda invalida turnos. */
@Service
public class ProcesarBajasTurnosPorCambioAgenda {

    private static final int LONGITUD_MAXIMA_MOTIVO = 255;

    private final BrechaHorariaRepository brechaHorariaRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final PoliticaTransicionesTurno politicaTransiciones;
    private final CalcularDisponibilidadDia calcularDisponibilidadDia;
    private final MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    private final DarDeBajaTurno darDeBajaTurno;
    private final ZoneId zonaHoraria;

    public ProcesarBajasTurnosPorCambioAgenda(
            BrechaHorariaRepository brechaHorariaRepository,
            ExcepcionAgendaRepository excepcionAgendaRepository,
            TurnoRepository turnoRepository,
            GestorCambioEstado gestorCambioEstado,
            PoliticaTransicionesTurno politicaTransiciones,
            CalcularDisponibilidadDia calcularDisponibilidadDia,
            MotivoBajaTurnoRepository motivoBajaTurnoRepository,
            DarDeBajaTurno darDeBajaTurno,
            @Value("${turnos.zona-horaria:America/Argentina/Buenos_Aires}") String zonaHoraria) {
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.politicaTransiciones = politicaTransiciones;
        this.calcularDisponibilidadDia = calcularDisponibilidadDia;
        this.motivoBajaTurnoRepository = motivoBajaTurnoRepository;
        this.darDeBajaTurno = darDeBajaTurno;
        this.zonaHoraria = ZoneId.of(zonaHoraria);
    }

    public List<Turno> ejecutar(DiaAgenda dia, String causa, String usuario) {
        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
        Long profesionalId = dia.getMesAgenda().getAgendaAnual().getProfesional().getId();
        List<IntervaloHorario> disponibilidad = calcularDisponibilidadDia.calcular(
                dia,
                gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA, dia.getId()),
                brechas,
                excepcionAgendaRepository.findActivasAplicablesAFecha(profesionalId, dia.getFecha()));

        List<Turno> afectados = new ArrayList<>();
        for (Turno turno : turnoRepository.findByDiaAgendaId(dia.getId())) {
            String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turno.getId());
            if (!politicaTransiciones.destinosPermitidos(estado)
                    .contains(PoliticaTransicionesTurno.DADO_DE_BAJA)) {
                continue;
            }
            IntervaloHorario intervalo = intervaloDelTurno(turno, dia);
            if (intervalo == null || disponibilidad.stream().noneMatch(d -> d.contiene(intervalo))) {
                afectados.add(turno);
            }
        }

        if (afectados.isEmpty()) {
            return List.of();
        }

        String textoMotivo = "Modificación de agenda: " + causa;
        if (textoMotivo.length() > LONGITUD_MAXIMA_MOTIVO) {
            textoMotivo = textoMotivo.substring(0, LONGITUD_MAXIMA_MOTIVO);
        }
        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo(textoMotivo);
        motivo = motivoBajaTurnoRepository.save(motivo);

        for (Turno turno : afectados) {
            darDeBajaTurno.ejecutar(turno.getId(), motivo, textoMotivo, usuario);
        }
        return List.copyOf(afectados);
    }

    private IntervaloHorario intervaloDelTurno(Turno turno, DiaAgenda dia) {
        ZonedDateTime inicio = turno.getInicioEstimado().atZone(zonaHoraria);
        ZonedDateTime fin = turno.getFinEstimado().atZone(zonaHoraria);
        if (!inicio.toLocalDate().equals(dia.getFecha()) || !fin.toLocalDate().equals(dia.getFecha())) {
            return null;
        }
        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();
        return horaInicio.isBefore(horaFin) ? new IntervaloHorario(horaInicio, horaFin) : null;
    }
}
