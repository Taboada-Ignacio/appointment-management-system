package com.apiturnos.turno.controller;

import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.shared.exception.DiaAgendaNoValidoException;
import com.apiturnos.turno.dto.CrearTurnoManualRequestDto;
import com.apiturnos.turno.dto.CrearTurnoManualResponseDto;
import com.apiturnos.turno.dto.HorarioSugeridoResponseDto;
import com.apiturnos.turno.service.CrearTurnoManual;
import com.apiturnos.turno.service.HorarioSugeridoTurnoManual;
import com.apiturnos.turno.service.ResultadoCrearTurnoManual;
import com.apiturnos.turno.service.SolicitudCrearTurnoManual;
import com.apiturnos.turno.service.SugerirHorariosTurnoManual;
import com.apiturnos.turno.service.ValidadorCrearTurnoManual;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/profesionales/{profesionalId}/turnos")
public class TurnoManualController {

    private final CrearTurnoManual crearTurnoManual;
    private final ValidadorCrearTurnoManual validadorCrearTurnoManual;
    private final SugerirHorariosTurnoManual sugerirHorariosTurnoManual;
    private final DiaAgendaRepository diaAgendaRepository;
    private final TipoAtencionRepository tipoAtencionRepository;
    private final Clock clock;

    public TurnoManualController(
            CrearTurnoManual crearTurnoManual,
            ValidadorCrearTurnoManual validadorCrearTurnoManual,
            SugerirHorariosTurnoManual sugerirHorariosTurnoManual,
            DiaAgendaRepository diaAgendaRepository,
            TipoAtencionRepository tipoAtencionRepository,
            Clock clock) {
        this.crearTurnoManual = crearTurnoManual;
        this.validadorCrearTurnoManual = validadorCrearTurnoManual;
        this.sugerirHorariosTurnoManual = sugerirHorariosTurnoManual;
        this.diaAgendaRepository = diaAgendaRepository;
        this.tipoAtencionRepository = tipoAtencionRepository;
        this.clock = clock;
    }

    @GetMapping("/horarios-sugeridos")
    public ResponseEntity<List<HorarioSugeridoResponseDto>> obtenerHorariosSugeridos(
            @PathVariable Long profesionalId,
            @RequestParam Long tipoAtencionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<HorarioSugeridoTurnoManual> sugerencias = sugerirHorariosTurnoManual.ejecutar(
                profesionalId, tipoAtencionId, fecha);

        List<HorarioSugeridoResponseDto> dtos = sugerencias.stream()
                .map(HorarioSugeridoResponseDto::new)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/validar")
    public ResponseEntity<CrearTurnoManualResponseDto> validarTurnoManual(
            @PathVariable Long profesionalId,
            @Valid @RequestBody CrearTurnoManualRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "profesional") String usuario) {

        Long diaAgendaId = resolverDiaAgendaId(profesionalId, request);
        SolicitudCrearTurnoManual solicitud = new SolicitudCrearTurnoManual(
                profesionalId,
                diaAgendaId,
                request.getClienteId(),
                request.getTipoAtencionId(),
                request.getInicioEstimado(),
                request.getFinEstimado(),
                false,
                request.getObservaciones(),
                usuario);

        ValidadorCrearTurnoManual.ContextoValidado contexto = validadorCrearTurnoManual.validar(solicitud);
        CrearTurnoManualResponseDto response = CrearTurnoManualResponseDto.fromContextoValidado(
                contexto,
                request.getInicioEstimado(),
                request.getFinEstimado(),
                request.getObservaciones());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CrearTurnoManualResponseDto> crearTurnoManual(
            @PathVariable Long profesionalId,
            @Valid @RequestBody CrearTurnoManualRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "profesional") String usuario) {

        Long diaAgendaId = resolverDiaAgendaId(profesionalId, request);
        SolicitudCrearTurnoManual solicitud = new SolicitudCrearTurnoManual(
                profesionalId,
                diaAgendaId,
                request.getClienteId(),
                request.getTipoAtencionId(),
                request.getInicioEstimado(),
                request.getFinEstimado(),
                request.isConfirmarAdvertencias(),
                request.getObservaciones(),
                usuario);

        ResultadoCrearTurnoManual resultado = crearTurnoManual.ejecutar(solicitud);

        TipoAtencion tipo = tipoAtencionRepository.findById(request.getTipoAtencionId()).orElse(null);
        Integer duracionMinutos = tipo != null ? tipo.getDuracionMinutos() : null;
        Integer capacidadSimultanea = tipo != null ? tipo.getCapacidadSimultanea() : null;

        CrearTurnoManualResponseDto responseDto = CrearTurnoManualResponseDto.fromResultado(
                resultado,
                request.getInicioEstimado(),
                request.getFinEstimado(),
                request.getObservaciones(),
                duracionMinutos,
                capacidadSimultanea);

        if (resultado.creado()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } else {
            return ResponseEntity.ok(responseDto);
        }
    }

    private Long resolverDiaAgendaId(Long profesionalId, CrearTurnoManualRequestDto request) {
        if (request.getDiaAgendaId() != null) {
            return request.getDiaAgendaId();
        }

        LocalDate fecha = request.getFecha();
        if (fecha == null && request.getInicioEstimado() != null) {
            fecha = request.getInicioEstimado().atZone(clock.getZone()).toLocalDate();
        }

        if (fecha == null) {
            throw new DiaAgendaNoValidoException("Debe especificarse el idDiaAgenda o la fecha del turno");
        }

        final LocalDate fechaFinal = fecha;
        return diaAgendaRepository.findByProfesionalIdAndFecha(profesionalId, fechaFinal)
                .map(DiaAgenda::getId)
                .orElseThrow(() -> new DiaAgendaNoValidoException(
                        "El día de agenda para la fecha " + fechaFinal + " no existe o no pertenece al profesional"));
    }
}
