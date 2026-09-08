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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.shared.exception.NegocioException;

@Service
public class CrearTurnoManual {

    private final ValidadorCrearTurnoManual validador;
    private final TurnoRepository turnoRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;
    private final TokenConfirmacionTurnoManual tokenConfirmacion;
    private final DiaAgendaRepository diaAgendaRepository;

    public CrearTurnoManual(
            ValidadorCrearTurnoManual validador,
            TurnoRepository turnoRepository,
            GestorCambioEstado gestorCambioEstado,
            RegistradorAuditoria registradorAuditoria,
            RegistradorNotificacion registradorNotificacion) {
        this(validador, turnoRepository, gestorCambioEstado, registradorAuditoria, registradorNotificacion, null, null);
    }

    @Autowired
    public CrearTurnoManual(
            ValidadorCrearTurnoManual validador,
            TurnoRepository turnoRepository,
            GestorCambioEstado gestorCambioEstado,
            RegistradorAuditoria registradorAuditoria,
            RegistradorNotificacion registradorNotificacion,
            TokenConfirmacionTurnoManual tokenConfirmacion,
            DiaAgendaRepository diaAgendaRepository) {
        this.validador = validador;
        this.turnoRepository = turnoRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
        this.tokenConfirmacion = tokenConfirmacion;
        this.diaAgendaRepository = diaAgendaRepository;
    }

    @Transactional
    public ResultadoCrearTurnoManual ejecutar(SolicitudCrearTurnoManual solicitud) {
        return ejecutar(solicitud, null);
    }

    public String emitirTokenConfirmacion(SolicitudCrearTurnoManual solicitud,
                                           ValidadorCrearTurnoManual.ContextoValidado contexto) {
        if (tokenConfirmacion == null || contexto.advertencias().isEmpty()) return null;
        return tokenConfirmacion.emitir(solicitud, contexto.advertencias(),
                contexto.capacidadMaxima(), contexto.turnosConcurrentes());
    }

    @Transactional
    public ResultadoCrearTurnoManual ejecutar(SolicitudCrearTurnoManual solicitud, String token) {
        if (diaAgendaRepository != null) {
            diaAgendaRepository.findByIdForUpdate(solicitud.diaAgendaId());
        }
        ValidadorCrearTurnoManual.ContextoValidado contexto = validador.validar(solicitud);

        if (!contexto.advertencias().isEmpty() && tokenConfirmacion != null) {
            // En produccion la confirmacion siempre queda vinculada a la
            // prevalidacion. El booleano se conserva en el contrato por
            // compatibilidad, pero ya no autoriza advertencias por si solo.
            if (token == null || token.isBlank()) {
                String emitido = tokenConfirmacion.emitir(solicitud, contexto.advertencias(),
                        contexto.capacidadMaxima(), contexto.turnosConcurrentes());
                return ResultadoCrearTurnoManual.requiereConfirmacion(
                        contexto.advertencias(), contexto.datosConfirmacion(), emitido);
            }
            if (token != null && !token.isBlank()) {
                try {
                    tokenConfirmacion.validarYConsumir(token, solicitud, contexto.advertencias(),
                            contexto.capacidadMaxima(), contexto.turnosConcurrentes());
                } catch (NegocioException confirmacionDesactualizada) {
                    String renovado = tokenConfirmacion.emitir(solicitud, contexto.advertencias(),
                            contexto.capacidadMaxima(), contexto.turnosConcurrentes());
                    return ResultadoCrearTurnoManual.requiereConfirmacion(
                            contexto.advertencias(), contexto.datosConfirmacion(), renovado);
                }
            }
        } else if (!contexto.advertencias().isEmpty() && !solicitud.confirmarAdvertencias()) {
            return ResultadoCrearTurnoManual.requiereConfirmacion(
                    contexto.advertencias(), contexto.datosConfirmacion());
        }

        Turno turno = new Turno();
        turno.setDiaAgenda(contexto.diaAgenda());
        turno.setCliente(contexto.cliente());
        turno.setTipoAtencion(contexto.tipoAtencion().getId() == null ? null : contexto.tipoAtencion());
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
                + "; duracionConfigurada=" + contexto.tipoAtencion().getDuracionMinutos()
                + "; duracionSolicitada=" + Duration.between(solicitud.inicioEstimado(), solicitud.finEstimado()).toMinutes()
                + "; capacidad=" + contexto.capacidadMaxima()
                + "; concurrenciaPrevia=" + contexto.turnosConcurrentes()
                + "; concurrenciaResultante=" + (contexto.turnosConcurrentes() + 1)
                + "; advertenciasConfirmadas=" + contexto.advertencias()
                + "; mecanismoConfirmacion=" + (token != null ? "TOKEN" : solicitud.confirmarAdvertencias() ? "LEGACY" : "NO_REQUERIDA");
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
