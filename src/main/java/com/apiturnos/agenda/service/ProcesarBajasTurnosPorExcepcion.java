package com.apiturnos.agenda.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcesarBajasTurnosPorExcepcion {

    private static final int LONGITUD_MAXIMA_MOTIVO = 255;

    private final MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorNotificacion registradorNotificacion;
    private final RegistradorAuditoria registradorAuditoria;

    public ProcesarBajasTurnosPorExcepcion(
            MotivoBajaTurnoRepository motivoBajaTurnoRepository,
            GestorCambioEstado gestorCambioEstado,
            RegistradorNotificacion registradorNotificacion,
            RegistradorAuditoria registradorAuditoria) {
        this.motivoBajaTurnoRepository = motivoBajaTurnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorNotificacion = registradorNotificacion;
        this.registradorAuditoria = registradorAuditoria;
    }

    /**
     * Procesa las bajas lógicas de los turnos afectados.
     * <p>
     * Crea un {@link MotivoBajaTurno} nuevo por cada ejecución efectiva que produzca bajas,
     * compartiéndolo entre todos los turnos afectados en esta ejecución y vinculándolo a la excepción,
     * garantizando la inmutabilidad de la causa histórica ante modificaciones posteriores de la excepción.
     * </p>
     */
    public void ejecutar(ExcepcionAgenda excepcion, List<Turno> turnosAfectados, String usuario) {
        if (turnosAfectados.isEmpty()) {
            return;
        }

        MotivoBajaTurno motivo = crearNuevoMotivo(excepcion);
        Long profesionalId = excepcion.getProfesional().getId();

        for (Turno turno : turnosAfectados) {
            String observacion = "Baja por excepción de agenda " + excepcion.getTipo()
                    + " (excepción " + excepcion.getId() + "): " + excepcion.getMotivo();

            gestorCambioEstado.registrarCambio(
                    AmbitoEstado.TURNO,
                    turno.getId(),
                    "DADO_DE_BAJA",
                    usuario,
                    observacion,
                    motivo);

            registradorNotificacion.registrarSiCorresponde(
                    turno.getCliente(),
                    turno,
                    TipoNotificacion.BAJA_TURNO,
                    "Su turno del " + turno.getDiaAgenda().getFecha()
                            + " ha sido dado de baja. Motivo: " + excepcion.getMotivo());

            registradorAuditoria.registrar(
                    "TURNO",
                    "Turno",
                    turno.getId(),
                    OperacionAuditoria.STATE_CHANGE,
                    usuario,
                    profesionalId,
                    "TURNO_DADO_DE_BAJA_POR_EXCEPCION: excepcionId=" + excepcion.getId()
                            + "; tipo=" + excepcion.getTipo());
        }
    }

    private MotivoBajaTurno crearNuevoMotivo(ExcepcionAgenda excepcion) {
        String texto = "Excepción de agenda " + excepcion.getTipo() + ": " + excepcion.getMotivo();
        if (texto.length() > LONGITUD_MAXIMA_MOTIVO) {
            texto = texto.substring(0, LONGITUD_MAXIMA_MOTIVO);
        }
        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo(texto);
        motivo.setExcepcionAgenda(excepcion);
        return motivoBajaTurnoRepository.save(motivo);
    }
}
