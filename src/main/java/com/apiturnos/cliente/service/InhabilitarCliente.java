package com.apiturnos.cliente.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InhabilitarCliente {

    private final ClienteRepository clienteRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public InhabilitarCliente(ClienteRepository clienteRepository,
                              GestorCambioEstado gestorCambioEstado,
                              RegistradorAuditoria registradorAuditoria) {
        this.clienteRepository = clienteRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Cliente ejecutar(Long clienteId, String observacion, String usuario) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Cliente", clienteId));

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, clienteId, "INHABILITADO", usuario, observacion, null);

        registradorAuditoria.registrar("CLIENTE", "Cliente", clienteId,
                OperacionAuditoria.STATE_CHANGE, usuario, cliente.getProfesional().getId(),
                "Cliente inhabilitado: " + observacion);

        return cliente;
    }
}
