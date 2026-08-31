package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.TipoNotificacion;
import com.apiturnos.notificacion.service.RegistradorNotificacion;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CrearTurno {

    private final TurnoRepository turnoRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final ClienteRepository clienteRepository;
    private final TipoAtencionRepository tipoAtencionRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final VerificarCapacidadTipoAtencion verificarCapacidadTipoAtencion;
    private final VerificadorCapacidad verificadorCapacidad;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public CrearTurno(TurnoRepository turnoRepository,
                      DiaAgendaRepository diaAgendaRepository,
                      ClienteRepository clienteRepository,
                      TipoAtencionRepository tipoAtencionRepository,
                      ConfiguracionRepository configuracionRepository,
                      GestorCambioEstado gestorCambioEstado,
                      VerificarCapacidadTipoAtencion verificarCapacidadTipoAtencion,
                      VerificadorCapacidad verificadorCapacidad,
                      RegistradorAuditoria registradorAuditoria,
                      RegistradorNotificacion registradorNotificacion) {
        this.turnoRepository = turnoRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.clienteRepository = clienteRepository;
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.configuracionRepository = configuracionRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.verificarCapacidadTipoAtencion = verificarCapacidadTipoAtencion;
        this.verificadorCapacidad = verificadorCapacidad;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
    }

    public static class Resultado {
        private final Turno turno;
        private final boolean capacidadExcedida;
        private final boolean requiereConfirmacion;
        private final String mensaje;

        public static Resultado exitoso(Turno turno, boolean capacidadExcedida) {
            return new Resultado(turno, capacidadExcedida, false, null);
        }

        public static Resultado requiereConfirmacion(String mensaje) {
            return new Resultado(null, true, true, mensaje);
        }

        public Resultado(Turno turno, boolean capacidadExcedida) {
            this(turno, capacidadExcedida, false, null);
        }

        public Resultado(Turno turno, boolean capacidadExcedida, boolean requiereConfirmacion, String mensaje) {
            this.turno = turno;
            this.capacidadExcedida = capacidadExcedida;
            this.requiereConfirmacion = requiereConfirmacion;
            this.mensaje = mensaje;
        }

        public Turno getTurno() { return turno; }
        public boolean isCapacidadExcedida() { return capacidadExcedida; }
        public boolean isRequiereConfirmacion() { return requiereConfirmacion; }
        public String getMensaje() { return mensaje; }
    }

    @Transactional
    public Resultado ejecutar(Long diaAgendaId, Long clienteId, Long tipoAtencionId,
                               Instant inicioEstimado, Instant finEstimado, OrigenTurno origen,
                               boolean confirmarSobrecapacidad, String observaciones,
                               String usuario) {
        DiaAgenda diaAgenda = diaAgendaRepository.findById(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", clienteId));

        Profesional profesionalAgenda = diaAgenda.getMesAgenda().getAgendaAnual().getProfesional();
        if (!profesionalAgenda.getId().equals(cliente.getProfesional().getId())) {
            throw new ClienteNoPerteneceProfesionalException(clienteId, profesionalAgenda.getId());
        }

        TipoAtencion tipoAtencion = null;
        boolean capacidadExcedida = false;

        if (tipoAtencionId != null) {
            tipoAtencion = tipoAtencionRepository.findById(tipoAtencionId)
                    .orElseThrow(() -> new EntidadNoEncontradaException("TipoAtencion", tipoAtencionId));

            if (!tipoAtencion.getProfesional().getId().equals(profesionalAgenda.getId())) {
                throw new TipoAtencionNoPerteneceProfesionalException(tipoAtencionId, profesionalAgenda.getId());
            }

            if (!tipoAtencion.isActivo()) {
                throw new NegocioException("El tipo de atención '" + tipoAtencion.getNombre() + "' está inactivo y no admite nuevos turnos");
            }

            if (origen == OrigenTurno.CLIENTE_AUTOGESTION) {
                verificarCapacidadTipoAtencion.verificarCapacidadAutoGestion(tipoAtencion, inicioEstimado, finEstimado);
            } else {
                boolean sobrecapacidad = verificarCapacidadTipoAtencion.esSobrecapacidadManual(tipoAtencion, inicioEstimado, finEstimado);
                if (sobrecapacidad) {
                    if (!confirmarSobrecapacidad) {
                        return Resultado.requiereConfirmacion(
                                "La capacidad simultánea (" + tipoAtencion.getCapacidadSimultanea() +
                                ") para " + tipoAtencion.getNombre() + " ha sido alcanzada. Se requiere confirmación.");
                    }
                    capacidadExcedida = true;
                }
            }
        } else {
            // Fallback para llamadas sin tipo de atención específico
            Configuracion config = configuracionRepository.findByProfesionalId(profesionalAgenda.getId())
                    .orElse(null);
            int maxSimultaneos = config != null ? config.getCantidadMaxTurnosALaVez() : 1;

            if (origen == OrigenTurno.CLIENTE_AUTOGESTION) {
                verificadorCapacidad.verificarCapacidadAutoGestion(
                        diaAgendaId, inicioEstimado, finEstimado, maxSimultaneos);
            } else {
                capacidadExcedida = verificadorCapacidad.excedidaCapacidadManual(
                        diaAgendaId, inicioEstimado, finEstimado, maxSimultaneos);
            }
        }

        // Determinar estado inicial según cliente
        String estadoCliente = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, clienteId);
        String estadoInicialTurno;
        if ("REQUIERE_APROBACION".equals(estadoCliente)) {
            estadoInicialTurno = "PENDIENTE_DE_APROBACION";
        } else if ("HABILITADO".equals(estadoCliente)) {
            estadoInicialTurno = "ASIGNADO";
        } else {
            throw new EstadoInvalidoException(
                    "El cliente " + clienteId + " tiene estado '" + estadoCliente +
                    "' y no puede solicitar turnos");
        }

        Turno turno = new Turno();
        turno.setDiaAgenda(diaAgenda);
        turno.setCliente(cliente);
        turno.setTipoAtencion(tipoAtencion);
        turno.setInicioEstimado(inicioEstimado);
        turno.setFinEstimado(finEstimado);
        turno.setOrigen(origen);
        turno.setObservaciones(observaciones);
        turno = turnoRepository.save(turno);

        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.TURNO, turno.getId(), estadoInicialTurno, usuario, "Turno creado");

        registradorAuditoria.registrar("TURNO", "Turno", turno.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalAgenda.getId(),
                "Turno creado con estado " + estadoInicialTurno +
                (tipoAtencion != null ? " para tipo " + tipoAtencion.getNombre() : ""));

        TipoNotificacion tipoNotif = "ASIGNADO".equals(estadoInicialTurno)
                ? TipoNotificacion.CONFIRMACION_TURNO
                : TipoNotificacion.CONFIRMACION_TURNO;
        registradorNotificacion.registrarSiCorresponde(cliente, turno, tipoNotif,
                "Su turno ha sido creado para el " + diaAgenda.getFecha());

        return Resultado.exitoso(turno, capacidadExcedida);
    }

    @Transactional
    public Resultado ejecutar(Long diaAgendaId, Long clienteId, Long tipoAtencionId,
                               Instant inicioEstimado, Instant finEstimado, OrigenTurno origen,
                               String observaciones, String usuario) {
        return ejecutar(diaAgendaId, clienteId, tipoAtencionId, inicioEstimado, finEstimado,
                origen, false, observaciones, usuario);
    }

    @Transactional
    public Resultado ejecutar(Long diaAgendaId, Long clienteId, Instant inicioEstimado,
                               Instant finEstimado, OrigenTurno origen, String observaciones,
                               String usuario) {
        return ejecutar(diaAgendaId, clienteId, null, inicioEstimado, finEstimado,
                origen, true, observaciones, usuario);
    }
}
