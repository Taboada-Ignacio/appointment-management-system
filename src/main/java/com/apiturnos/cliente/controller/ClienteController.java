package com.apiturnos.cliente.controller;

import com.apiturnos.cliente.dto.ClienteDetalleDto;
import com.apiturnos.cliente.dto.ClienteResumenDto;
import com.apiturnos.cliente.dto.ClienteRequestDto;
import com.apiturnos.cliente.dto.BajaClienteRequestDto;
import com.apiturnos.cliente.service.ListarCarteraClientes;
import com.apiturnos.cliente.service.ObtenerCliente;
import com.apiturnos.cliente.service.RegistrarCliente;
import com.apiturnos.cliente.service.EditarCliente;
import com.apiturnos.cliente.service.DarDeBajaCliente;
import com.apiturnos.cliente.model.Cliente;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final RegistrarCliente registrarCliente;
    private final EditarCliente editarCliente;
    private final DarDeBajaCliente darDeBajaCliente;

    public ClienteController(ListarCarteraClientes listarCarteraClientes,
                             ObtenerCliente obtenerCliente,
                             RegistrarCliente registrarCliente,
                             EditarCliente editarCliente,
                             DarDeBajaCliente darDeBajaCliente) {
        this.listarCarteraClientes = listarCarteraClientes;
        this.obtenerCliente = obtenerCliente;
        this.registrarCliente = registrarCliente;
        this.editarCliente = editarCliente;
        this.darDeBajaCliente = darDeBajaCliente;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ClienteDetalleDto> crear(
            @PathVariable Long profesionalId,
            @Valid @RequestBody ClienteRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "profesional") String usuario) {
        Cliente cliente = registrarCliente.ejecutar(profesionalId, request.getNombre(), request.getApellido(),
                request.getTipoDocumento(), request.getNumeroDocumento(), request.getEmail(), request.getTelefono(),
                false, usuario);
        ClienteDetalleDto response = obtenerCliente.ejecutar(profesionalId, cliente.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/profesionales/" + profesionalId + "/clientes/" + cliente.getId())
                .body(response);
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

    @PutMapping("/{clienteId}")
    @Transactional
    public ResponseEntity<ClienteDetalleDto> editar(
            @PathVariable Long profesionalId,
            @PathVariable Long clienteId,
            @Valid @RequestBody ClienteRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "profesional") String usuario) {
        editarCliente.ejecutar(profesionalId, clienteId, request.getNombre(), request.getApellido(),
                request.getTipoDocumento(), request.getNumeroDocumento(), request.getEmail(), request.getTelefono(),
                request.getNotificacionesHabilitadas(), usuario);
        return ResponseEntity.ok(obtenerCliente.ejecutar(profesionalId, clienteId));
    }

    @DeleteMapping("/{clienteId}")
    @Transactional
    public ResponseEntity<Void> darDeBaja(
            @PathVariable Long profesionalId,
            @PathVariable Long clienteId,
            @Valid @RequestBody(required = false) BajaClienteRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "profesional") String usuario) {
        darDeBajaCliente.ejecutar(profesionalId, clienteId,
                request != null ? request.getMotivo() : null, usuario);
        return ResponseEntity.noContent().build();
    }
}

