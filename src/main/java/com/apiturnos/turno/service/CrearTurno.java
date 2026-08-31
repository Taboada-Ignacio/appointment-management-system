package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
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
    private final ConfiguracionRepository configuracionRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final VerificadorCapacidad verificadorCapacidad;
    private final RegistradorAuditoria registradorAuditoria;
    private final RegistradorNotificacion registradorNotificacion;

    public CrearTurno(TurnoRepository turnoRepository,
                      DiaAgendaRepository diaAgendaRepository,
                      ClienteRepository clienteRepository,
                      ConfiguracionRepository configuracionRepository,
                      GestorCambioEstado gestorCambioEstado,
                      VerificadorCapacidad verificadorCapacidad,
                      RegistradorAuditoria registradorAuditoria,
                      RegistradorNotificacion registradorNotificacion) {
        this.turnoRepository = turnoRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.clienteRepository = clienteRepository;
        this.configuracionRepository = configuracionRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.verificadorCapacidad = verificadorCapacidad;
        this.registradorAuditoria = registradorAuditoria;
        this.registradorNotificacion = registradorNotificacion;
    }

    public static class Resultado {
        private final Turno turno;
        private final boolean capacidadExcedida;

        public Resultado(Turno turno, boolean capacidadExcedida) {
            this.turno = turno;
            this.capacidadExcedida = capacidadExcedida;
        }

        public Turno getTurno() { return turno; }
        public boolean isCapacidadExcedida() { return capacidadExcedida; }
    }

    @Transactional
    public Resultado ejecutar(Long diaAgendaId, Long clienteId, Instant inicioEstimado,
                               Instant finEstimado, OrigenTurno origen, String observaciones,
                               String usuario) {
        DiaAgenda diaAgenda = diaAgendaRepository.findById(diaAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("DiaAgenda", diaAgendaId));

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", clienteId));

        // Navigate to profesional: diaAgenda -> mesAgenda -> agendaAnual -> profesional
        Profesional profesionalAgenda = diaAgenda.getMesAgenda().getAgendaAnual().getProfesional();
        if (!profesionalAgenda.getId().equals(cliente.getProfesional().getId())) {
            throw new ClienteNoPerteneceProfesionalException(clienteId, profesionalAgenda.getId());
        }

        // Get configuracion for capacity check
        Configuracion config = configuracionRepository.findByProfesionalId(profesionalAgenda.getId())
                .orElse(null);
        int maxSimultaneos = config != null ? config.getCantidadMaxTurnosALaVez() : 1;

        boolean capacidadExcedida = false;
        if (origen == OrigenTurno.CLIENTE_AUTOGESTION) {
            // Strict: throws CapacidadAgotadaException if exceeded
            verificadorCapacidad.verificarCapacidadAutoGestion(
                    diaAgendaId, inicioEstimado, finEstimado, maxSimultaneos);
        } else {
            // Manual: just warn
            capacidadExcedida = verificadorCapacidad.excedidaCapacidadManual(
                    diaAgendaId, inicioEstimado, finEstimado, maxSimultaneos);
        }

        // Determine initial state based on client's current state
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

        // Create Turno
        Turno turno = new Turno();
        turno.setDiaAgenda(diaAgenda);
        turno.setCliente(cliente);
        turno.setInicioEstimado(inicioEstimado);
        turno.setFinEstimado(finEstimado);
        turno.setOrigen(origen);
        turno.setObservaciones(observaciones);
        turno = turnoRepository.save(turno);

        // Register initial state
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.TURNO, turno.getId(), estadoInicialTurno, usuario, "Turno creado");

        // Audit
        registradorAuditoria.registrar("TURNO", "Turno", turno.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalAgenda.getId(),
                "Turno creado con estado " + estadoInicialTurno);

        // Notification
        TipoNotificacion tipoNotif = "ASIGNADO".equals(estadoInicialTurno)
                ? TipoNotificacion.CONFIRMACION_TURNO
                : TipoNotificacion.CONFIRMACION_TURNO;
        registradorNotificacion.registrarSiCorresponde(cliente, turno, tipoNotif,
                "Su turno ha sido creado para el " + diaAgenda.getFecha());

        return new Resultado(turno, capacidadExcedida);
    }
}
