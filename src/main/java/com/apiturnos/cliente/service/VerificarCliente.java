package com.apiturnos.cliente.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoEncontradoException;
import com.apiturnos.shared.exception.ClienteNoPendienteDeVerificacionException;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.NegocioException;
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
    public Cliente ejecutar(Long profesionalId, Long clienteId, String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (clienteId == null) {
            throw new NegocioException("El ID del cliente es obligatorio");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(profesionalId, clienteId));

        if (!cliente.getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(clienteId, profesionalId);
        }

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, clienteId);
        if (!"PENDIENTE_DE_VERIFICACION".equals(estadoActual)) {
            throw new ClienteNoPendienteDeVerificacionException(clienteId, estadoActual);
        }

        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE, clienteId, "HABILITADO", usuario, "Cliente verificado", null);

        registradorAuditoria.registrar("CLIENTE", "Cliente", clienteId,
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId,
                "CLIENTE_VERIFICADO: PENDIENTE_DE_VERIFICACION → HABILITADO");

        return cliente;
    }

    @Transactional
    public Cliente ejecutar(Long clienteId, String usuario) {
        if (clienteId == null) {
            throw new NegocioException("El ID del cliente es obligatorio");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(clienteId));
        return ejecutar(cliente.getProfesional().getId(), clienteId, usuario);
    }
}
