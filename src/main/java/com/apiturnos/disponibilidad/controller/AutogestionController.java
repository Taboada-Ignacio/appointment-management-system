package com.apiturnos.disponibilidad.controller;

import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.dto.TipoAtencionResponseDto;
import com.apiturnos.disponibilidad.dto.CrearTurnoAutogestionRequestDto;
import com.apiturnos.disponibilidad.dto.CrearTurnoAutogestionResponseDto;
import com.apiturnos.disponibilidad.dto.SlotDisponibleDto;
import com.apiturnos.disponibilidad.service.CalcularSlotsDisponiblesAutogestion;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.service.CrearTurno;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/autogestion/profesionales/{profesionalId}")
public class AutogestionController {
    private final CalcularSlotsDisponiblesAutogestion calcularSlots;
    private final CrearTurno crearTurno;
    private final TipoAtencionRepository tipoAtencionRepository;

    public AutogestionController(CalcularSlotsDisponiblesAutogestion calcularSlots,
                                 CrearTurno crearTurno,
                                 TipoAtencionRepository tipoAtencionRepository) {
        this.calcularSlots = calcularSlots;
        this.crearTurno = crearTurno;
        this.tipoAtencionRepository = tipoAtencionRepository;
    }

    @GetMapping("/tipos-atencion/{tipoAtencionId}/slots")
    public List<SlotDisponibleDto> slots(
            @PathVariable Long profesionalId,
            @PathVariable Long tipoAtencionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return calcularSlots.ejecutar(profesionalId, tipoAtencionId, fecha);
    }

    @GetMapping("/tipos-atencion")
    public List<TipoAtencionResponseDto> tiposActivos(@PathVariable Long profesionalId) {
        return tipoAtencionRepository.findByProfesionalIdAndActivoTrueOrderByIdAsc(profesionalId)
                .stream().map(TipoAtencionResponseDto::new).toList();
    }

    @PostMapping("/turnos")
    public ResponseEntity<CrearTurnoAutogestionResponseDto> reservar(
            @PathVariable Long profesionalId,
            @Valid @RequestBody CrearTurnoAutogestionRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "cliente-autogestion") String usuario) {
        TipoAtencion tipo = tipoAtencionRepository
                .findByIdAndProfesionalId(request.tipoAtencionId(), profesionalId)
                .orElseThrow(() -> new EntidadNoEncontradaException("TipoAtencion", request.tipoAtencionId()));
        Instant fin = request.inicioEstimado().plusSeconds(tipo.getDuracionMinutos() * 60L);
        CrearTurno.Resultado resultado = crearTurno.ejecutar(
                request.diaAgendaId(), request.clienteId(), request.tipoAtencionId(),
                request.inicioEstimado(), fin, OrigenTurno.CLIENTE_AUTOGESTION,
                false, request.observaciones(), usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CrearTurnoAutogestionResponseDto(
                resultado.getTurno().getId(), "ASIGNADO", request.inicioEstimado(), fin));
    }
}
