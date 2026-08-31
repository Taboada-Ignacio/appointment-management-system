package com.apiturnos.cliente.controller;

import com.apiturnos.cliente.dto.ClienteDetalleDto;
import com.apiturnos.cliente.dto.ClienteResumenDto;
import com.apiturnos.cliente.service.ListarCarteraClientes;
import com.apiturnos.cliente.service.ObtenerCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/clientes")
@Transactional(readOnly = true)
public class ClienteController {

    private final ListarCarteraClientes listarCarteraClientes;
    private final ObtenerCliente obtenerCliente;

    public ClienteController(ListarCarteraClientes listarCarteraClientes,
                             ObtenerCliente obtenerCliente) {
        this.listarCarteraClientes = listarCarteraClientes;
        this.obtenerCliente = obtenerCliente;
    }

    @GetMapping
    public ResponseEntity<Page<ClienteResumenDto>> listarCartera(
            @PathVariable Long profesionalId,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ClienteResumenDto> clientes = listarCarteraClientes.ejecutar(
                profesionalId, nombre, apellido, dni, estado, pageable);
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{clienteId}")
    public ResponseEntity<ClienteDetalleDto> obtenerPorId(
            @PathVariable Long profesionalId,
            @PathVariable Long clienteId) {
        ClienteDetalleDto cliente = obtenerCliente.ejecutar(profesionalId, clienteId);
        return ResponseEntity.ok(cliente);
    }
}

