package com.apiturnos.cliente.controller;

import com.apiturnos.cliente.service.CambiarEstadoCliente;
import com.apiturnos.cliente.service.ListarCarteraClientes;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.NegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/clientes/pendientes-verificacion")
public class ClientesPendientesController {
    private final ListarCarteraClientes listar;
    private final CambiarEstadoCliente cambiar;
    private final GestorCambioEstado estados;
    private final JdbcTemplate jdbc;
    public ClientesPendientesController(ListarCarteraClientes listar, CambiarEstadoCliente cambiar,
            GestorCambioEstado estados, JdbcTemplate jdbc) {
        this.listar=listar; this.cambiar=cambiar; this.estados=estados; this.jdbc=jdbc;
    }
    @GetMapping("/ids")
    public List<Long> ids(@PathVariable Long profesionalId) {
        return listar.ejecutar(profesionalId,null,null,null,"PENDIENTE_DE_VERIFICACION",Pageable.unpaged())
                .getContent().stream().map(cliente -> cliente.getId()).toList();
    }
    public record Seleccion(@NotEmpty List<@NotNull @Positive Long> clienteIds, @NotBlank String estado) {}
    @PostMapping("/estado")
    @Transactional
    public ResponseEntity<Void> actualizar(@PathVariable Long profesionalId, @Valid @RequestBody Seleccion solicitud,
            @RequestHeader(value="X-Usuario", defaultValue="profesional") String usuario) {
        if (!Set.of("HABILITADO","INHABILITADO","REQUIERE_APROBACION","DADO_DE_BAJA").contains(solicitud.estado()))
            throw new NegocioException("La acción sobre clientes pendientes no es válida");
        List<Long> ids=solicitud.clienteIds().stream().distinct().sorted().toList();
        for (Long id : ids) {
            var propietarios=jdbc.queryForList("SELECT profesional_id FROM cliente WHERE id=? FOR UPDATE",Long.class,id);
            if (propietarios.isEmpty() || !profesionalId.equals(propietarios.getFirst()))
                throw new NegocioException("El cliente no pertenece al profesional");
            if (!"PENDIENTE_DE_VERIFICACION".equals(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,id)))
                throw new NegocioException("Un cliente seleccionado ya no está pendiente de verificación. Actualizá la lista.");
        }
        for (Long id : ids) cambiar.ejecutar(profesionalId,id,solicitud.estado(),"Acción sobre clientes pendientes",usuario);
        return ResponseEntity.noContent().build();
    }
}
