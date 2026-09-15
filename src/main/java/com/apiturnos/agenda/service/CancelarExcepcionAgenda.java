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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class CancelarExcepcionAgenda {

    private final ExcepcionAgendaRepository excepcionAgendaRepository;
    private final RegistradorAuditoria registradorAuditoria;
    private final SincronizarEstadoDiasPorExcepcion sincronizarDias;
    private final AfectacionTurnoExcepcionRepository afectaciones;
    private final GestorCambioEstado estados;

    public CancelarExcepcionAgenda(ExcepcionAgendaRepository excepcionAgendaRepository,
                                    RegistradorAuditoria registradorAuditoria,
                                    SincronizarEstadoDiasPorExcepcion sincronizarDias,
                                    AfectacionTurnoExcepcionRepository afectaciones,
                                    GestorCambioEstado estados) {
        this.excepcionAgendaRepository = excepcionAgendaRepository;
        this.registradorAuditoria = registradorAuditoria;
        this.sincronizarDias = sincronizarDias;
        this.afectaciones = afectaciones;
        this.estados = estados;
    }

    @Transactional
    public ExcepcionAgenda ejecutar(Long profesionalId, Long excepcionId, String usuario) {
        ExcepcionAgenda excepcion = excepcionAgendaRepository
                .findByIdAndProfesionalId(excepcionId, profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("ExcepcionAgenda", excepcionId));

        if (excepcion.isActiva()) {
            var fechasAfectadas = SincronizarEstadoDiasPorExcepcion.fechasEfectivas(excepcion);
            excepcion.setActiva(false);
            excepcion = excepcionAgendaRepository.save(excepcion);
            sincronizarDias.reconciliar(profesionalId, fechasAfectadas, usuario);
            for (var afectacion : afectaciones.findByExcepcionAgendaIdOrderByIdAsc(excepcionId)) {
                if (afectacion.getEstadoResolucion() != EstadoResolucionAfectacion.PENDIENTE) continue;
                Long turnoId = afectacion.getTurno().getId();
                if (!PoliticaTransicionesTurno.AFECTADO_POR_EXCEPCION.equals(
                        estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turnoId))) continue;
                estados.registrarCambio(AmbitoEstado.TURNO, turnoId,
                        afectacion.getEstadoTurnoAnterior(), usuario,
                        "Restaurado por cancelación de excepción " + excepcionId, null);
                afectacion.setEstadoResolucion(EstadoResolucionAfectacion.RESTAURADO);
                afectacion.setResueltoEn(Instant.now());
                afectaciones.save(afectacion);
            }
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
}
