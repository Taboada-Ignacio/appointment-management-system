package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.EstadoResolucionAfectacion;
import com.apiturnos.agenda.repository.AfectacionTurnoExcepcionRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.turno.service.PoliticaTransicionesTurno;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class CancelarExcepcionAgenda {

    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final RegistradorAuditoria registradorAuditoria;
    private final SincronizarEstadoDiasPorExcepcion sincronizarDias;
    private final AfectacionTurnoExcepcionRepository afectaciones;
    private final GestorCambioEstado estados;
    private final MotivoBajaTurnoRepository motivosBaja;
    private final Clock clock;

    public CancelarExcepcionAgenda(ExcepcionAgendaRepository excepcionAgendaRepository,
                                    RegistradorAuditoria registradorAuditoria,
                                    SincronizarEstadoDiasPorExcepcion sincronizarDias,
                                    AfectacionTurnoExcepcionRepository afectaciones,
                                    GestorCambioEstado estados,
                                    MotivoBajaTurnoRepository motivosBaja,
                                    Clock clock) {
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.registradorAuditoria = registradorAuditoria;
        this.sincronizarDias = sincronizarDias;
        this.afectaciones = afectaciones;
        this.estados = estados;
        this.motivosBaja = motivosBaja;
        this.clock = clock;
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId, Long excepcionId, String usuario) {
        ExcepcionAgenda excepcion = excepcionAgendaRepository
                .findByIdAndProfesionalId(excepcionId, profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("ExcepcionAgenda", excepcionId));

        LocalDate hoy = LocalDate.now(clock);
        if (hoy.isAfter(excepcion.getFechaFin())) {
            throw new NegocioException("La excepción " + excepcionId + " ya finalizó y no puede darse de baja");
        }
        if (hoy.isBefore(excepcion.getFechaInicio())) {
            eliminarFutura(excepcion, profesionalId, usuario);
            return excepcion;
        }

        if (excepcion.isActiva()) {
            var fechasAfectadas = SincronizarEstadoDiasPorExcepcion.fechasEfectivas(excepcion);
            excepcion.setActiva(false);
            excepcion = excepcionAgendaRepository.save(excepcion);
            sincronizarDias.reconciliar(profesionalId, fechasAfectadas, usuario);
            restaurarPendientes(excepcion, usuario);
            registradorAuditoria.registrar(
                    "AGENDA",
                    "ExcepcionAgenda",
                    excepcion.getId(),
                    OperacionAuditoria.CANCEL,
                    usuario,
                    profesionalId,
                    "EXCEPCION_AGENDA_CANCELADA: tipo=" + excepcion.getTipo()
                            + "; turnos pendientes restaurados; los turnos dados de baja no se reactivan");
        }

        return excepcion;
    }

    private void eliminarFutura(ExcepcionAgenda excepcion, Long profesionalId, String usuario) {
        Long excepcionId = excepcion.getId();
        sincronizarDias.reconciliar(profesionalId,
                SincronizarEstadoDiasPorExcepcion.fechasEfectivas(excepcion), usuario);
        restaurarPendientes(excepcion, usuario);
        motivosBaja.desvincularExcepcion(excepcionId);
        afectaciones.deleteByExcepcionAgendaId(excepcionId);
        excepcionAgendaRepository.delete(excepcion);
        registradorAuditoria.registrar(
                "AGENDA", "ExcepcionAgenda", excepcionId, OperacionAuditoria.DELETE,
                usuario, profesionalId,
                "EXCEPCION_AGENDA_ELIMINADA_ANTICIPADAMENTE: tipo=" + excepcion.getTipo());
    }

    private void restaurarPendientes(ExcepcionAgenda excepcion, String usuario) {
        for (var afectacion : afectaciones.findByExcepcionAgendaIdOrderByIdAsc(excepcion.getId())) {
            if (afectacion.getEstadoResolucion() != EstadoResolucionAfectacion.PENDIENTE) continue;
            Long turnoId = afectacion.getTurno().getId();
            if (!PoliticaTransicionesTurno.AFECTADO_POR_EXCEPCION.equals(
                    estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turnoId))) continue;
            estados.registrarCambio(AmbitoEstado.TURNO, turnoId,
                    afectacion.getEstadoTurnoAnterior(), usuario,
                    "Restaurado por cancelación de excepción " + excepcion.getId(), null);
            afectacion.setEstadoResolucion(EstadoResolucionAfectacion.RESTAURADO);
            afectacion.setResueltoEn(Instant.now());
            afectaciones.save(afectacion);
        }
    }
}
