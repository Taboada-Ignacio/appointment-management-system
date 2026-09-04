package com.apiturnos.agenda.controller;

import com.apiturnos.agenda.dto.ExcepcionAgendaRequestDto;
import com.apiturnos.agenda.dto.ExcepcionAgendaResponseDto;
import com.apiturnos.agenda.dto.ImpactoExcepcionAgendaResponseDto;
import com.apiturnos.agenda.dto.ResultadoExcepcionAgendaResponseDto;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.service.AplicarExcepcionAgenda;
import com.apiturnos.agenda.service.AplicarExcepcionConResoluciones;
import com.apiturnos.agenda.service.CancelarExcepcionAgenda;
import com.apiturnos.agenda.service.ConstructorImpactoExcepcionAgenda;
import com.apiturnos.agenda.service.ModificarExcepcionAgenda;
import com.apiturnos.agenda.service.PrevisualizarExcepcionAgenda;
import com.apiturnos.agenda.service.ResultadoAplicacionExcepcionAgenda;
import com.apiturnos.agenda.service.TokenImpactoExcepcionAgenda;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/excepciones-agenda")
@Tag(name = "Excepciones de agenda", description = "Previsualización y gestión de cambios excepcionales sin alterar la agenda base")
@Transactional(readOnly = true)
public class ExcepcionAgendaController {

    private final ExcepcionAgendaRepository repository;
    private final PrevisualizarExcepcionAgenda previsualizar;
    private final AplicarExcepcionAgenda aplicar;
    private final ModificarExcepcionAgenda modificar;
    private final CancelarExcepcionAgenda cancelar;
    private final ConstructorImpactoExcepcionAgenda constructorImpacto;
    private final TokenImpactoExcepcionAgenda tokenImpacto;
    private final AplicarExcepcionConResoluciones aplicarConResoluciones;

    public ExcepcionAgendaController(
            ExcepcionAgendaRepository repository,
            PrevisualizarExcepcionAgenda previsualizar,
            AplicarExcepcionAgenda aplicar,
            ModificarExcepcionAgenda modificar,
            CancelarExcepcionAgenda cancelar,
            ConstructorImpactoExcepcionAgenda constructorImpacto,
            TokenImpactoExcepcionAgenda tokenImpacto,
            AplicarExcepcionConResoluciones aplicarConResoluciones) {
        this.repository = repository;
        this.previsualizar = previsualizar;
        this.aplicar = aplicar;
        this.modificar = modificar;
        this.cancelar = cancelar;
        this.constructorImpacto = constructorImpacto;
        this.tokenImpacto = tokenImpacto;
        this.aplicarConResoluciones = aplicarConResoluciones;
    }

    @GetMapping
    @Operation(summary = "Listar excepciones", description = "Admite rango inclusivo y filtro por estado activo")
    public ResponseEntity<List<ExcepcionAgendaResponseDto>> listar(
            @PathVariable Long profesionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Boolean activa) {
        validarRango(desde, hasta);
        List<ExcepcionAgenda> excepciones = repository.findByProfesionalId(profesionalId);
        return ResponseEntity.ok(excepciones.stream()
                .filter(e -> desde == null || !e.getFechaFin().isBefore(desde))
                .filter(e -> hasta == null || !e.getFechaInicio().isAfter(hasta))
                .filter(e -> activa == null || e.isActiva() == activa)
                .sorted(Comparator.comparing(ExcepcionAgenda::getFechaInicio, Comparator.reverseOrder())
                        .thenComparing(Comparator.comparing(ExcepcionAgenda::getId, Comparator.reverseOrder())))
                .map(ExcepcionAgendaResponseDto::from)
                .toList());
    }

    @GetMapping("/{excepcionId}")
    @Operation(summary = "Obtener una excepción")
    public ResponseEntity<ExcepcionAgendaResponseDto> obtener(
            @PathVariable Long profesionalId,
            @PathVariable Long excepcionId) {
        return ResponseEntity.ok(ExcepcionAgendaResponseDto.from(buscar(profesionalId, excepcionId)));
    }

    @PostMapping("/preview")
    @Operation(summary = "Previsualizar una nueva excepción", description = "No modifica turnos ni genera notificaciones")
    public ResponseEntity<ImpactoExcepcionAgendaResponseDto> previsualizarNueva(
            @PathVariable Long profesionalId,
            @Valid @RequestBody ExcepcionAgendaRequestDto request) {
        var solicitud = request.toSolicitud();
        var turnos = previsualizar.nueva(profesionalId, solicitud);
        return ResponseEntity.ok(constructorImpacto.construir(
                tokenImpacto.generar(solicitud, turnos), turnos));
    }

    @PostMapping("/{excepcionId}/preview")
    @Operation(summary = "Previsualizar la modificación de una excepción", description = "Reemplaza la versión vigente sólo para el cálculo; no persiste cambios")
    public ResponseEntity<ImpactoExcepcionAgendaResponseDto> previsualizarModificacion(
            @PathVariable Long profesionalId,
            @PathVariable Long excepcionId,
            @Valid @RequestBody ExcepcionAgendaRequestDto request) {
        var solicitud = request.toSolicitud();
        var turnos = previsualizar.modificacion(profesionalId, excepcionId, solicitud);
        return ResponseEntity.ok(constructorImpacto.construir(
                tokenImpacto.generar(solicitud, turnos), turnos));
    }

    @PostMapping
    @Operation(summary = "Crear y aplicar una excepción", description = "Recalcula el impacto y da de baja los turnos afectados atómicamente")
    @Transactional
    public ResponseEntity<ResultadoExcepcionAgendaResponseDto> crear(
            @PathVariable Long profesionalId,
            @Valid @RequestBody ExcepcionAgendaRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        ResultadoAplicacionExcepcionAgenda resultado = request.previewToken() == null
                ? aplicar.ejecutarConResultado(profesionalId, request.toSolicitud(), usuario)
                : aplicarConResoluciones.ejecutar(
                    profesionalId, request.toSolicitud(), request.previewToken(), request.decisiones(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertir(resultado));
    }

    @PutMapping("/{excepcionId}")
    @Operation(summary = "Modificar y reaplicar una excepción", description = "Puede generar nuevas bajas; nunca reactiva turnos dados de baja")
    @Transactional
    public ResponseEntity<ResultadoExcepcionAgendaResponseDto> modificar(
            @PathVariable Long profesionalId,
            @PathVariable Long excepcionId,
            @Valid @RequestBody ExcepcionAgendaRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        return ResponseEntity.ok(convertir(modificar.ejecutarConResultado(
                profesionalId, excepcionId, request.toSolicitud(), request.previewToken(), usuario)));
    }

    @DeleteMapping("/{excepcionId}")
    @Operation(summary = "Cancelar una excepción", description = "La desactiva; no reactiva turnos previamente dados de baja")
    @Transactional
    public ResponseEntity<ExcepcionAgendaResponseDto> cancelar(
            @PathVariable Long profesionalId,
            @PathVariable Long excepcionId,
            @RequestHeader(value = "X-Usuario", defaultValue = "admin") String usuario) {
        return ResponseEntity.ok(ExcepcionAgendaResponseDto.from(
                cancelar.ejecutar(profesionalId, excepcionId, usuario)));
    }

    private ExcepcionAgenda buscar(Long profesionalId, Long excepcionId) {
        return repository.findByIdAndProfesionalId(excepcionId, profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("ExcepcionAgenda", excepcionId));
    }

    private ResultadoExcepcionAgendaResponseDto convertir(ResultadoAplicacionExcepcionAgenda resultado) {
        return new ResultadoExcepcionAgendaResponseDto(
                ExcepcionAgendaResponseDto.from(resultado.excepcion()),
                constructorImpacto.construir(resultado.turnosAfectados()));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde debe ser anterior o igual a la fecha hasta");
        }
    }
}
