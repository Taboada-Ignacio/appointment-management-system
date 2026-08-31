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
public class VerificarCliente {

    private final ClienteRepository clienteRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public VerificarCliente(ClienteRepository clienteRepository,
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

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, clienteId, "HABILITADO", usuario, "Cliente verificado", null);

        registradorAuditoria.registrar("CLIENTE", "Cliente", clienteId,
                OperacionAuditoria.STATE_CHANGE, usuario, cliente.getProfesional().getId(),
                "Cliente verificado: PENDIENTE_DE_VERIFICACION → HABILITADO");

        return cliente;
    }
}
