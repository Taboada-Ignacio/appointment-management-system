package com.apiturnos.cliente.service;

import com.apiturnos.cliente.dto.ClienteDetalleDto;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoEncontradoException;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerCliente {

    private final ClienteRepository clienteRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public ObtenerCliente(ClienteRepository clienteRepository,
                          GestorCambioEstado gestorCambioEstado) {
        this.clienteRepository = clienteRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @Transactional(readOnly = true)
    public ClienteDetalleDto ejecutar(Long profesionalId, Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(profesionalId, clienteId));

        if (!cliente.getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(clienteId, profesionalId);
        }

        String estadoActual = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, clienteId);
        return new ClienteDetalleDto(cliente, estadoActual);
    }
}

