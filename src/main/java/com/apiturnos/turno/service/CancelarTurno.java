package com.apiturnos.turno.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.TurnoNoPerteneceProfesionalException;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelarTurno {

    private final TurnoRepository turnoRepository;
    private final TurnoHistorialRepository turnoHistorialRepository;
    private final CambioEstadoRepository cambioEstadoRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;
    private final PoliticaCancelacionTurno politicaCancelacion;

    public CancelarTurno(TurnoRepository turnoRepository,
                         TurnoHistorialRepository turnoHistorialRepository,
                         CambioEstadoRepository cambioEstadoRepository,
                         ConfiguracionRepository configuracionRepository,
                         MotivoBajaTurnoRepository motivoBajaTurnoRepository,
                         GestorCambioEstado gestorCambioEstado,
                         RegistradorAuditoria registradorAuditoria,
                         RegistradorNotificacion registradorNotificacion,
                         PoliticaCancelacionTurno politicaCancelacion) {
        this.turnoRepository = turnoRepository;
        this.turnoHistorialRepository = turnoHistorialRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.configuracionRepository = configuracionRepository;
        this.motivoBajaTurnoRepository = motivoBajaTurnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
        this.politicaCancelacion = politicaCancelacion;
    }

    @Transactional
    public ResultadoCancelacionTurno ejecutar(Long profesionalId, Long turnoId, String motivoTexto, String usuario) {
        return ejecutarInterno(profesionalId, turnoId, motivoTexto, usuario);
    }

    @Transactional
    public ResultadoCancelacionTurno ejecutar(Long turnoId, String motivoTexto, String usuario) {
        return ejecutarInterno(null, turnoId, motivoTexto, usuario);
    }

    private ResultadoCancelacionTurno ejecutarInterno(Long profesionalIdEsperado, Long turnoId,
                                                      String motivoTexto, String usuario) {
        Turno turno = turnoRepository.findByIdForUpdate(turnoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Turno", turnoId));

        Long profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
        if (profesionalIdEsperado != null && !profesionalId.equals(profesionalIdEsperado)) {
            throw new TurnoNoPerteneceProfesionalException(turnoId, profesionalIdEsperado);
        }

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.TURNO, turnoId);
        gestorCambioEstado.validarTransicion(
                AmbitoEstado.TURNO, estadoActual, PoliticaTransicionesTurno.CANCELADO);

        Configuracion configuracion = configuracionRepository.findByProfesionalId(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Configuracion del Profesional", profesionalId));
        TipoResolucionCancelacion resolucion = politicaCancelacion.resolver(
                turnoId, turno.getInicioEstimado(), configuracion);

        if (resolucion == TipoResolucionCancelacion.ELIMINACION_ANTICIPADA) {
            eliminarAnticipadamente(turno, profesionalId, usuario);
            return new ResultadoCancelacionTurno(turnoId, resolucion);
        }

        if (motivoTexto == null || motivoTexto.isBlank()) {
            throw new EstadoInvalidoException("Cancelar un Turno requiere MotivoBajaTurno");
        }
        String motivoNormalizado = motivoTexto.trim();

        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo(motivoNormalizado);
        motivo = motivoBajaTurnoRepository.save(motivo);

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.TURNO, turnoId, PoliticaTransicionesTurno.CANCELADO,
                usuario, motivoNormalizado, motivo);

        registradorAuditoria.registrar("TURNO", "Turno", turnoId,
                OperacionAuditoria.CANCEL, usuario, profesionalId,
                "TURNO_CANCELADO: " + motivoNormalizado);

        registradorNotificacion.registrarSiCorresponde(turno.getCliente(), turno,
                TipoNotificacion.CANCELACION_TURNO,
                "Su turno del " + turno.getDiaAgenda().getFecha()
                        + " ha sido cancelado. Motivo: " + motivoNormalizado);

        return new ResultadoCancelacionTurno(turnoId, resolucion);
    }

    private void eliminarAnticipadamente(Turno turno, Long profesionalId, String usuario) {
        Long turnoId = turno.getId();
        registradorAuditoria.registrar(
                "TURNO", "Turno", turnoId, OperacionAuditoria.DELETE, usuario, profesionalId,
                "TURNO_ELIMINADO_ANTICIPADAMENTE: inicioEstimado=" + turno.getInicioEstimado()
                        + "; clienteId=" + turno.getCliente().getId()
                        + "; diaAgendaId=" + turno.getDiaAgenda().getId());

        registradorNotificacion.registrarSiCorresponde(
                // El turno será eliminado en esta misma transacción. La intención se conserva
                // sin FK, igual que ocurriría tras un ON DELETE SET NULL.
                turno.getCliente(), null, TipoNotificacion.CANCELACION_TURNO,
                "Su turno del " + turno.getDiaAgenda().getFecha() + " ha sido cancelado anticipadamente.");

        turnoHistorialRepository.deleteByTurnoId(turnoId);
        turnoHistorialRepository.flush();
        cambioEstadoRepository.deleteByAmbitoAndEntidadId(AmbitoEstado.TURNO, turnoId);
        cambioEstadoRepository.flush();
        turnoRepository.delete(turno);
        turnoRepository.flush();

    }
}
