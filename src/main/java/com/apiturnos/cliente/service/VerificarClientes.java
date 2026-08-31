package com.apiturnos.cliente.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.ClienteNoEncontradoException;
import com.apiturnos.shared.exception.ClienteNoPendienteDeVerificacionException;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VerificarClientes {

    private final ClienteRepository clienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public VerificarClientes(ClienteRepository clienteRepository,
                             ProfesionalRepository profesionalRepository,
                             GestorCambioEstado gestorCambioEstado,
                             RegistradorAuditoria registradorAuditoria) {
        this.clienteRepository = clienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public List<Cliente> ejecutar(Long profesionalId, List<Long> clienteIds, String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (clienteIds == null || clienteIds.isEmpty()) {
            return List.of();
        }

        if (!profesionalRepository.existsById(profesionalId)) {
            throw new EntidadNoEncontradaException("Profesional", profesionalId);
        }

        List<Cliente> clientesAceptados = new ArrayList<>();

        // Fase 1: Validación previa de todos los clientes (política atómica)
        for (Long clienteId : clienteIds) {
            Cliente cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new ClienteNoEncontradoException(profesionalId, clienteId));

            if (!cliente.getProfesional().getId().equals(profesionalId)) {
                throw new ClienteNoPerteneceProfesionalException(clienteId, profesionalId);
            }

            String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, clienteId);
            if (!"PENDIENTE_DE_VERIFICACION".equals(estadoActual)) {
                throw new ClienteNoPendienteDeVerificacionException(clienteId, estadoActual);
            }

            clientesAceptados.add(cliente);
        }

        // Fase 2: Aplicación atómica de cambios de estado
        for (Cliente cliente : clientesAceptados) {
            gestorCambioEstado.registrarCambio(
                    AmbitoEstado.CLIENTE, cliente.getId(), "HABILITADO", usuario, "Verificación masiva", null);
        }

        registradorAuditoria.registrar("CLIENTE", "Cliente", profesionalId,
                OperacionAuditoria.STATE_CHANGE, usuario, profesionalId,
                "CLIENTES_VERIFICADOS: " + clienteIds.size() + " clientes verificados (" + clienteIds + ")");

        return clientesAceptados;
    }
}

