package com.apiturnos.cliente.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.shared.exception.ClienteDuplicadoException;
import com.apiturnos.shared.exception.ClienteNoEncontradoException;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EditarCliente {

    private final ClienteRepository clienteRepository;
    private final RegistradorAuditoria registradorAuditoria;

    public EditarCliente(ClienteRepository clienteRepository,
                         RegistradorAuditoria registradorAuditoria) {
        this.clienteRepository = clienteRepository;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Cliente ejecutar(Long profesionalId, Long clienteId, String nombre, String apellido,
                            TipoDocumento tipoDocumento, String numeroDocumento,
                            String email, String telefono, Boolean notificacionesHabilitadas,
                            String usuario) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (clienteId == null) {
            throw new NegocioException("El ID del cliente es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre del cliente es obligatorio");
        }
        if (apellido == null || apellido.isBlank()) {
            throw new NegocioException("El apellido del cliente es obligatorio");
        }
        if (tipoDocumento == null) {
            throw new NegocioException("El tipo de documento es obligatorio");
        }
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new NegocioException("El número de documento es obligatorio");
        }
        if (email == null || email.isBlank()) {
            throw new NegocioException("El email del cliente es obligatorio");
        }
        if (telefono == null || telefono.isBlank()) {
            throw new NegocioException("El teléfono del cliente es obligatorio");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(profesionalId, clienteId));

        if (!cliente.getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(clienteId, profesionalId);
        }

        String dniNormalizado = numeroDocumento.trim();

        // Si el DNI cambió o es diferente, validar que no colisione con otro cliente del mismo profesional
        if (!dniNormalizado.equalsIgnoreCase(cliente.getNumeroDocumento()) &&
                clienteRepository.existsByProfesionalIdAndNumeroDocumentoAndIdNot(profesionalId, dniNormalizado, clienteId)) {
            throw new ClienteDuplicadoException(profesionalId, dniNormalizado);
        }

        cliente.setNombre(nombre.trim());
        cliente.setApellido(apellido.trim());
        cliente.setTipoDocumento(tipoDocumento);
        cliente.setNumeroDocumento(dniNormalizado);
        cliente.setEmail(email.trim().toLowerCase());
        cliente.setTelefono(telefono.trim());
        if (notificacionesHabilitadas != null) {
            cliente.setNotificacionesHabilitadas(notificacionesHabilitadas);
        }

        cliente = clienteRepository.save(cliente);

        registradorAuditoria.registrar("CLIENTE", "Cliente", clienteId,
                OperacionAuditoria.UPDATE, usuario, profesionalId,
                "CLIENTE_EDITADO: Datos personales actualizados");

        return cliente;
    }
}

