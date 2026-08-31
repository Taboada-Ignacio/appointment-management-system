package com.apiturnos.cliente.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoEncontradoException;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EstadoClienteInvalidoException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class CambiarEstadoCliente {

    private static final Set<String> ESTADOS_CLIENTE_VALIDOS = Set.of(
            "HABILITADO",
            "PENDIENTE_DE_VERIFICACION",
            "REQUIERE_APROBACION",
            "INHABILITADO",
            "DADO_DE_BAJA"
    );

    private final ClienteRepository clienteRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public CambiarEstadoCliente(ClienteRepository clienteRepository,
                                GestorCambioEstado gestorCambioEstado,
                                RegistradorAuditoria registradorAuditoria) {
        this.clienteRepository = clienteRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Cliente ejecutar(Long profesionalId, Long clienteId, String nuevoEstado,
                            String observacion, String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (clienteId == null) {
            throw new NegocioException("El ID del cliente es obligatorio");
        }
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new NegocioException("El nuevo estado es obligatorio");
        }

        String estadoDestino = nuevoEstado.trim().toUpperCase();
        if (!ESTADOS_CLIENTE_VALIDOS.contains(estadoDestino)) {
            throw new EstadoClienteInvalidoException(
                    "El estado '" + estadoDestino + "' no es un estado válido para el ámbito CLIENTE");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(profesionalId, clienteId));

        if (!cliente.getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(clienteId, profesionalId);
        }

        String estadoAnterior = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, clienteId);

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, clienteId, estadoDestino, usuario, observacion, null);

        registradorAuditoria.registrar("CLIENTE", "Cliente", clienteId,
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId,
                "ESTADO_CLIENTE_MODIFICADO: " + estadoAnterior + " → " + estadoDestino +
                (observacion != null ? " (" + observacion + ")" : ""));

        return cliente;
    }
}

