package com.apiturnos.turno.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrearTurnoManual {

    private final ValidadorCrearTurnoManual validador;
    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public CrearTurnoManual(
            ValidadorCrearTurnoManual validador,
            TurnoRepository turnoRepository,
            GestorCambioEstado gestorCambioEstado,
            RegistradorAuditoria registradorAuditoria,
            RegistradorNotificacion registradorNotificacion) {
        this.validador = validador;
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
    }

    @Transactional
    public ResultadoCrearTurnoManual ejecutar(SolicitudCrearTurnoManual solicitud) {
        ValidadorCrearTurnoManual.ContextoValidado contexto = validador.validar(solicitud);

        if (!contexto.advertencias().isEmpty() && !solicitud.confirmarAdvertencias()) {
            return ResultadoCrearTurnoManual.requiereConfirmacion(
                    contexto.advertencias(), contexto.datosConfirmacion());
        }

        Turno turno = new Turno();
        turno.setDiaAgenda(contexto.diaAgenda());
        turno.setCliente(contexto.cliente());
        turno.setTipoAtencion(contexto.tipoAtencion());
        turno.setInicioEstimado(solicitud.inicioEstimado());
        turno.setFinEstimado(solicitud.finEstimado());
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno.setObservaciones(solicitud.observaciones());
        turno = turnoRepository.save(turno);

        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.TURNO,
                turno.getId(),
                "ASIGNADO",
                solicitud.usuario(),
                "Turno creado manualmente por el profesional");

        String detalleAuditoria = "TURNO_CREADO_MANUALMENTE; estado=ASIGNADO; tipoAtencion="
                + contexto.tipoAtencion().getNombre()
                + "; advertencias=" + contexto.advertencias();
        registradorAuditoria.registrar(
                "TURNO",
                "Turno",
                turno.getId(),
                OperacionAuditoria.CREATE,
                solicitud.usuario(),
                solicitud.profesionalId(),
                detalleAuditoria);

        registradorNotificacion.registrarSiCorresponde(
                contexto.cliente(),
                turno,
                TipoNotificacion.CONFIRMACION_TURNO,
                "Su turno ha sido asignado para el " + contexto.diaAgenda().getFecha()
                        + " a las " + contexto.datosConfirmacion().horaInicio());

        return ResultadoCrearTurnoManual.creado(
                turno.getId(), contexto.advertencias(), contexto.datosConfirmacion());
    }
}
