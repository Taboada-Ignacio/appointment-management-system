package com.apiturnos.cliente.service;

import com.apiturnos.cliente.dto.ClienteResumenDto;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ListarCarteraClientes {

    private final ClienteRepository clienteRepository;
    private final ProfesionalRepository profesionalRepository;
    private final GestorCambioEstado gestorCambioEstado;

    public ListarCarteraClientes(ClienteRepository clienteRepository,
                                 ProfesionalRepository profesionalRepository,
                                 GestorCambioEstado gestorCambioEstado) {
        this.clienteRepository = clienteRepository;
        this.profesionalRepository = profesionalRepository;
        this.gestorCambioEstado = gestorCambioEstado;
    }

    @Transactional(readOnly = true)
    public Page<ClienteResumenDto> ejecutar(Long profesionalId,
                                           String nombre,
                                           String apellido,
                                           String dni,
                                           String estado,
                                           Pageable pageable) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }

        if (!profesionalRepository.existsById(profesionalId)) {
            throw new EntidadNoEncontradaException("Profesional", profesionalId);
        }

        String nombreFiltro = (nombre != null && !nombre.isBlank()) ? "%" + nombre.trim().toLowerCase() + "%" : null;
        String apellidoFiltro = (apellido != null && !apellido.isBlank()) ? "%" + apellido.trim().toLowerCase() + "%" : null;
        String dniFiltro = (dni != null && !dni.isBlank()) ? "%" + dni.trim() + "%" : null;
        String estadoFiltro = (estado != null && !estado.isBlank()) ? estado.trim() : null;

        Page<Cliente> paginaClientes = clienteRepository.buscarCartera(
                profesionalId, nombreFiltro, apellidoFiltro, dniFiltro, estadoFiltro, pageable);

        List<Long> clienteIds = paginaClientes.getContent().stream()
                .map(Cliente::getId)
                .toList();

        // Evitar N+1: cargar en batch los estados actuales de los clientes en la página
        Map<Long, String> estadosMap = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.CLIENTE, clienteIds);

        List<ClienteResumenDto> dtos = paginaClientes.getContent().stream()
                .map(cliente -> new ClienteResumenDto(cliente, estadosMap.get(cliente.getId())))
                .toList();

        return new PageImpl<>(dtos, pageable, paginaClientes.getTotalElements());
    }
}
