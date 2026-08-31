package com.apiturnos.cliente.service;

import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.shared.exception.NegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BuscarClientes {

    private final ClienteRepository clienteRepository;

    public BuscarClientes(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorDni(Long profesionalId, String dni) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (dni == null || dni.isBlank()) {
            return Optional.empty();
        }
        String dniNormalizado = dni.trim();
        return clienteRepository.findByProfesionalIdAndNumeroDocumento(profesionalId, dniNormalizado);
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarPorApellido(Long profesionalId, String apellido) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (apellido == null || apellido.isBlank()) {
            return List.of();
        }
        return clienteRepository.findByProfesionalIdAndApellidoContainingIgnoreCase(profesionalId, apellido.trim());
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarPorNombre(Long profesionalId, String nombre) {
        if (profesionalId == null) {
            throw new NegocioException("El ID del profesional es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            return List.of();
        }
        return clienteRepository.findByProfesionalIdAndNombreContainingIgnoreCase(profesionalId, nombre.trim());
    }
}

