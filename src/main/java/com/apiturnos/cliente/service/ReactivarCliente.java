package com.apiturnos.cliente.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.EstadoInvalidoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReactivarCliente {

    private final ClienteRepository clienteRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public ReactivarCliente(ClienteRepository clienteRepository,
                            GestorCambioEstado gestorCambioEstado,
                            RegistradorAuditoria registradorAuditoria) {
        this.clienteRepository = clienteRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Cliente ejecutar(Long clienteId, String usuario) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", clienteId));

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, clienteId);
        if (!"DADO_DE_BAJA".equals(estadoActual)) {
            throw new EstadoInvalidoException(
                    "Solo se puede reactivar un cliente DADO_DE_BAJA. Estado actual: " + estadoActual);
        }

        // Find the state before DADO_DE_BAJA in the history
        List<CambioEstado> historial = gestorCambioEstado.obtenerHistorial(AmbitoEstado.CLIENTE, clienteId);
        String estadoAnterior = "HABILITADO"; // default fallback
        for (int i = historial.size() - 1; i >= 0; i--) {
            String nombre = historial.get(i).getEstado().getNombre();
            if (!"DADO_DE_BAJA".equals(nombre)) {
                estadoAnterior = nombre;
                break;
            }
        }

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, clienteId, estadoAnterior, usuario,
                "Cliente reactivado al estado anterior: " + estadoAnterior, null);

        registradorAuditoria.registrar("CLIENTE", "Cliente", clienteId,
                OperacionAuditoria.STATE_CHANGE, usuario, cliente.getProfesional().getId(),
                "Cliente reactivado: DADO_DE_BAJA → " + estadoAnterior);

        return cliente;
    }
}
