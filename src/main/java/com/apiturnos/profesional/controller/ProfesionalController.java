package com.apiturnos.profesional.controller;

import com.apiturnos.profesional.dto.ConfiguracionRequestDto;
import com.apiturnos.profesional.dto.ConfiguracionResponseDto;
import com.apiturnos.profesional.dto.ProfesionalRequestDto;
import com.apiturnos.profesional.dto.ProfesionalResponseDto;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.service.EditarProfesional;
import com.apiturnos.profesional.service.EliminarConfiguracionProfesional;
import com.apiturnos.profesional.service.EliminarProfesional;
import com.apiturnos.profesional.service.ListarProfesionales;
import com.apiturnos.profesional.service.ModificarConfiguracionProfesional;
import com.apiturnos.profesional.service.ObtenerConfiguracionProfesional;
import com.apiturnos.profesional.service.ObtenerProfesional;
import com.apiturnos.profesional.service.RegistrarConfiguracionProfesional;
import com.apiturnos.profesional.service.RegistrarProfesional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/profesionales")
@Transactional(readOnly = true)
@Tag(name = "Profesionales", description = "Alta, consulta, edición y eliminación de profesionales")
public class ProfesionalController {

    private final RegistrarProfesional registrarProfesional;
    private final ObtenerProfesional obtenerProfesional;
    private final ListarProfesionales listarProfesionales;
    private final EditarProfesional editarProfesional;
    private final EliminarProfesional eliminarProfesional;
    private final ObtenerConfiguracionProfesional obtenerConfiguracionProfesional;
    private final ModificarConfiguracionProfesional modificarConfiguracionProfesional;
    private final RegistrarConfiguracionProfesional registrarConfiguracionProfesional;
    private final EliminarConfiguracionProfesional eliminarConfiguracionProfesional;

    public ProfesionalController(RegistrarProfesional registrarProfesional,
                                 ObtenerProfesional obtenerProfesional,
                                 ListarProfesionales listarProfesionales,
                                 EditarProfesional editarProfesional,
                                 EliminarProfesional eliminarProfesional,
                                 ObtenerConfiguracionProfesional obtenerConfiguracionProfesional,
                                 ModificarConfiguracionProfesional modificarConfiguracionProfesional,
                                 RegistrarConfiguracionProfesional registrarConfiguracionProfesional,
                                 EliminarConfiguracionProfesional eliminarConfiguracionProfesional) {
        this.registrarProfesional = registrarProfesional;
        this.obtenerProfesional = obtenerProfesional;
        this.listarProfesionales = listarProfesionales;
        this.editarProfesional = editarProfesional;
        this.eliminarProfesional = eliminarProfesional;
        this.obtenerConfiguracionProfesional = obtenerConfiguracionProfesional;
        this.modificarConfiguracionProfesional = modificarConfiguracionProfesional;
        this.registrarConfiguracionProfesional = registrarConfiguracionProfesional;
        this.eliminarConfiguracionProfesional = eliminarConfiguracionProfesional;
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Registrar un profesional", description = "Crea únicamente el profesional; no genera su configuración")
    public ResponseEntity<ProfesionalResponseDto> registrar(
            @Valid @RequestBody ProfesionalRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        Profesional profesional = registrarProfesional.ejecutar(
                request.getNombre(),
                request.getApellido(),
                request.getEmail(),
                request.getTelefono(),
                request.getEspecialidad(),
                usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProfesionalResponseDto(profesional));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un profesional por ID")
    public ResponseEntity<ProfesionalResponseDto> obtenerPorId(@PathVariable Long id) {
        Profesional profesional = obtenerProfesional.ejecutar(id);
        return ResponseEntity.ok(new ProfesionalResponseDto(profesional));
    }

    @GetMapping
    @Operation(summary = "Listar todos los profesionales", description = "No requiere parámetros")
    public ResponseEntity<List<ProfesionalResponseDto>> listar() {
        return ResponseEntity.ok(listarProfesionales.ejecutar());
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar profesionales", description = "Permite búsqueda general, filtro por especialidad y paginación")
    public ResponseEntity<Page<ProfesionalResponseDto>> buscar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String especialidad,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProfesionalResponseDto> profesionales = listarProfesionales.ejecutar(busqueda, especialidad, pageable);
        return ResponseEntity.ok(profesionales);
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Editar un profesional")
    public ResponseEntity<ProfesionalResponseDto> editar(
            @PathVariable Long id,
            @Valid @RequestBody ProfesionalRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        Profesional profesional = editarProfesional.ejecutar(
                id,
                request.getNombre(),
                request.getApellido(),
                request.getEmail(),
                request.getTelefono(),
                request.getEspecialidad(),
                usuario);
        return ResponseEntity.ok(new ProfesionalResponseDto(profesional));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Eliminar un profesional", description = "Elimina su configuración 1:1 si existe y rechaza la operación si conserva otras relaciones")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        eliminarProfesional.ejecutar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/configuracion")
    @Operation(summary = "Obtener la configuración del profesional")
    public ResponseEntity<ConfiguracionResponseDto> obtenerConfiguracion(@PathVariable Long id) {
        Configuracion configuracion = obtenerConfiguracionProfesional.ejecutar(id);
        return ResponseEntity.ok(new ConfiguracionResponseDto(configuracion));
    }

    @PostMapping("/{id}/configuracion")
    @Transactional
    @Operation(summary = "Registrar la configuración del profesional", description = "Crea la configuración inicial para un profesional existente")
    public ResponseEntity<ConfiguracionResponseDto> registrarConfiguracion(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ConfiguracionRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        ConfiguracionRequestDto body = request != null ? request : new ConfiguracionRequestDto();
        Configuracion configuracion = registrarConfiguracionProfesional.ejecutar(
                id,
                body.getCantidadMaxTurnosALaVez(),
                body.getDuracionAproximadaPorTurno(),
                body.getAgendaSoloManejadaPorProfesional(),
                body.getUmbralCancelacionHoras(),
                usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ConfiguracionResponseDto(configuracion));
    }

    @PutMapping("/{id}/configuracion")
    @Transactional
    @Operation(summary = "Modificar la configuración del profesional")
    public ResponseEntity<ConfiguracionResponseDto> modificarConfiguracion(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracionRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        Configuracion configuracion = modificarConfiguracionProfesional.ejecutar(
                id,
                request.getCantidadMaxTurnosALaVez(),
                request.getDuracionAproximadaPorTurno(),
                request.getAgendaSoloManejadaPorProfesional(),
                request.getUmbralCancelacionHoras(),
                usuario);
        return ResponseEntity.ok(new ConfiguracionResponseDto(configuracion));
    }

    @DeleteMapping("/{id}/configuracion")
    @Transactional
    @Operation(summary = "Eliminar la configuración del profesional (pruebas)", description = "Elimina la configuración de un profesional por su ID. Uso exclusivo para testing.")
    public ResponseEntity<Void> eliminarConfiguracion(
            @PathVariable Long id,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        eliminarConfiguracionProfesional.ejecutar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
