package com.apiturnos.cliente.controller;

import com.apiturnos.cliente.service.CambiarEstadoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/clientes/{clienteId}/estado")
public class EstadoClienteController {
    private final CambiarEstadoCliente cambiar;
    public EstadoClienteController(CambiarEstadoCliente cambiar) { this.cambiar = cambiar; }
    public record Solicitud(@NotBlank String estado, @Size(max=255) String observacion) {}
    @PutMapping
    public ResponseEntity<Void> cambiar(@PathVariable Long profesionalId, @PathVariable Long clienteId,
            @Valid @RequestBody Solicitud solicitud,
            @RequestHeader(value="X-Usuario", defaultValue="profesional") String usuario) {
        cambiar.ejecutar(profesionalId, clienteId, solicitud.estado(), solicitud.observacion(), usuario);
        return ResponseEntity.noContent().build();
    }
}
