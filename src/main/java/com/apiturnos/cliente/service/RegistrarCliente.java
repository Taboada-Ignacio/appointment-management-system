package com.apiturnos.cliente.service;

import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.ClienteDuplicadoException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.OrigenTurno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrarCliente {

    private final ClienteRepository clienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final RegistradorAuditoria registradorAuditoria;

    public RegistrarCliente(ClienteRepository clienteRepository,
                            ProfesionalRepository profesionalRepository,
                            GestorCambioEstado gestorCambioEstado,
                            RegistradorAuditoria registradorAuditoria) {
        this.clienteRepository = clienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.registradorAuditoria = registradorAuditoria;
    }

    @Transactional
    public Cliente ejecutar(Long profesionalId, String nombre, String apellido,
                            TipoDocumento tipoDocumento, String numeroDocumento,
                            String email, String telefono, boolean esAutoregistro,
                            String usuario) {
        Profesional profesional = profesionalRepository.findById(profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Profesional", profesionalId));

        if (clienteRepository.existsByProfesionalIdAndNumeroDocumento(profesionalId, numeroDocumento)) {
            throw new ClienteDuplicadoException(profesionalId, numeroDocumento);
        }

        Cliente cliente = new Cliente();
        cliente.setProfesional(profesional);
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTipoDocumento(tipoDocumento);
        cliente.setNumeroDocumento(numeroDocumento);
        cliente.setEmail(email);
        cliente.setTelefono(telefono);
        cliente.setNotificacionesHabilitadas(true);
        cliente = clienteRepository.save(cliente);

        String estadoInicial = esAutoregistro ? "PENDIENTE_DE_VERIFICACION" : "HABILITADO";
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.CLIENTE, cliente.getId(), estadoInicial, usuario, "Alta de cliente");

        registradorAuditoria.registrar("CLIENTE", "Cliente", cliente.getId(),
                OperacionAuditoria.CREATE, usuario, profesionalId,
                "Cliente registrado con estado " + estadoInicial);

        return cliente;
    }
}
