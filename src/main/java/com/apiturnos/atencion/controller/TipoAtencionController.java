package com.apiturnos.atencion.controller;

import com.apiturnos.atencion.dto.TipoAtencionRequestDto;
import com.apiturnos.atencion.dto.TipoAtencionResponseDto;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.service.ActivarTipoAtencion;
import com.apiturnos.atencion.service.EditarTipoAtencion;
import com.apiturnos.atencion.service.InactivarTipoAtencion;
import com.apiturnos.atencion.service.ListarTiposAtencion;
import com.apiturnos.atencion.service.ObtenerTipoAtencion;
import com.apiturnos.atencion.service.RegistrarTipoAtencion;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/tipos-atencion")
@Transactional(readOnly = true)
public class TipoAtencionController {

    private final RegistrarTipoAtencion registrarTipoAtencion;
    private final EditarTipoAtencion editarTipoAtencion;
    private final ActivarTipoAtencion activarTipoAtencion;
    private final InactivarTipoAtencion inactivarTipoAtencion;
    private final ListarTiposAtencion listarTiposAtencion;
    private final ObtenerTipoAtencion obtenerTipoAtencion;

    public TipoAtencionController(RegistrarTipoAtencion registrarTipoAtencion,
                                  EditarTipoAtencion editarTipoAtencion,
                                  ActivarTipoAtencion activarTipoAtencion,
                                  InactivarTipoAtencion inactivarTipoAtencion,
                                  ListarTiposAtencion listarTiposAtencion,
                                  ObtenerTipoAtencion obtenerTipoAtencion) {
        this.registrarTipoAtencion = registrarTipoAtencion;
        this.editarTipoAtencion = editarTipoAtencion;
        this.activarTipoAtencion = activarTipoAtencion;
        this.inactivarTipoAtencion = inactivarTipoAtencion;
        this.listarTiposAtencion = listarTiposAtencion;
        this.obtenerTipoAtencion = obtenerTipoAtencion;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TipoAtencionResponseDto> registrar(
            @PathVariable Long profesionalId,
            @Valid @RequestBody TipoAtencionRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(
                profesionalId,
                request.getNombre(),
                request.getDescripcion(),
                request.getDuracionMinutos(),
                request.getCapacidadSimultanea(),
                usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TipoAtencionResponseDto(tipo));
    }

    @GetMapping
    public ResponseEntity<List<TipoAtencionResponseDto>> listar(
            @PathVariable Long profesionalId,
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos) {
        List<TipoAtencion> tipos = listarTiposAtencion.ejecutar(profesionalId, soloActivos);
        List<TipoAtencionResponseDto> dtos = tipos.stream()
                .map(TipoAtencionResponseDto::new)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoAtencionResponseDto> obtenerPorId(
            @PathVariable Long profesionalId,
            @PathVariable Long id) {
        TipoAtencion tipo = obtenerTipoAtencion.ejecutar(profesionalId, id);
        return ResponseEntity.ok(new TipoAtencionResponseDto(tipo));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<TipoAtencionResponseDto> editar(
            @PathVariable Long profesionalId,
            @PathVariable Long id,
            @Valid @RequestBody TipoAtencionRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        TipoAtencion tipo = editarTipoAtencion.ejecutar(
                profesionalId,
                id,
                request.getNombre(),
                request.getDescripcion(),
                request.getDuracionMinutos(),
                request.getCapacidadSimultanea(),
                usuario);
        return ResponseEntity.ok(new TipoAtencionResponseDto(tipo));
    }

    @PatchMapping("/{id}/activar")
    @Transactional
    public ResponseEntity<TipoAtencionResponseDto> activar(
            @PathVariable Long profesionalId,
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        TipoAtencion tipo = activarTipoAtencion.ejecutar(profesionalId, id, usuario);
        return ResponseEntity.ok(new TipoAtencionResponseDto(tipo));
    }

    @PatchMapping("/{id}/inactivar")
    @Transactional
    public ResponseEntity<TipoAtencionResponseDto> inactivar(
            @PathVariable Long profesionalId,
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        TipoAtencion tipo = inactivarTipoAtencion.ejecutar(profesionalId, id, usuario);
        return ResponseEntity.ok(new TipoAtencionResponseDto(tipo));
    }
}

