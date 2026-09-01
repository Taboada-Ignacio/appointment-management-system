package com.apiturnos.turno.controller;

import com.apiturnos.turno.dto.CancelarTurnoRequestDto;
import com.apiturnos.turno.dto.CancelarTurnoResponseDto;
import com.apiturnos.turno.dto.DarDeBajaTurnoRequestDto;
import com.apiturnos.turno.dto.TurnoResponseDto;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.service.CancelarTurno;
import com.apiturnos.turno.service.DarDeBajaTurno;
import com.apiturnos.turno.service.PoliticaTransicionesTurno;
import com.apiturnos.turno.service.ResultadoCancelacionTurno;
import com.apiturnos.turno.service.TipoResolucionCancelacion;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para operaciones de ciclo de vida de Turnos (Cancelación y Baja Administrativa).
 */
@RestController
@RequestMapping("/api/profesionales/{profesionalId}/turnos")
@Transactional(readOnly = true)
public class TurnoController {

    private final CancelarTurno cancelarTurno;
    private final DarDeBajaTurno darDeBajaTurno;
    private final TurnoRepository turnoRepository;

    public TurnoController(CancelarTurno cancelarTurno,
                           DarDeBajaTurno darDeBajaTurno,
                           TurnoRepository turnoRepository) {
        this.cancelarTurno = cancelarTurno;
        this.darDeBajaTurno = darDeBajaTurno;
        this.turnoRepository = turnoRepository;
    }

    /**
     * Endpoint de cancelación ordinaria de un Turno.
     * <p>
     * El backend decide automáticamente si aplica ELIMINACION_ANTICIPADA o CANCELADO con registro histórico
     * según el umbral temporal configurado por el profesional.
     * </p>
     *
     * @param profesionalId ID del profesional propietario del turno
     * @param turnoId       ID del turno a cancelar
     * @param request       DTO con motivo opcional (obligatorio si la operación cae dentro del umbral)
     * @param usuario       Identificador del usuario que ejecuta la acción
     * @return DTO con el resultado de la resolución ("ELIMINADO_ANTICIPADAMENTE" o "CANCELADO")
     */
    @PostMapping("/{turnoId}/cancelacion")
    @Transactional
    public ResponseEntity<CancelarTurnoResponseDto> cancelar(
            @PathVariable Long profesionalId,
            @PathVariable Long turnoId,
            @RequestBody(required = false) CancelarTurnoRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "profesional") String usuario) {

        String motivo = request != null ? request.getMotivo() : null;
        ResultadoCancelacionTurno resultado = cancelarTurno.ejecutar(profesionalId, turnoId, motivo, usuario);

        if (resultado.resolucion() == TipoResolucionCancelacion.ELIMINACION_ANTICIPADA) {
            return ResponseEntity.ok(CancelarTurnoResponseDto.eliminadoAnticipadamente(turnoId));
        }

        Turno turno = turnoRepository.findByIdConRelaciones(turnoId).orElse(null);
        TurnoResponseDto turnoDto = TurnoResponseDto.from(turno, PoliticaTransicionesTurno.CANCELADO);
        return ResponseEntity.ok(CancelarTurnoResponseDto.canceladoConHistorial(turnoId, turnoDto));
    }

    /**
     * Endpoint para dar de baja administrativa un Turno.
     * <p>
     * Aplica la transición a DADO_DE_BAJA sin evaluar umbrales temporales. Requiere un motivo explicativo.
     * </p>
     *
     * @param profesionalId ID del profesional propietario del turno
     * @param turnoId       ID del turno a dar de baja
     * @param request       DTO con motivo obligatorio de la baja
     * @param usuario       Identificador del usuario que ejecuta la acción
     * @return DTO del turno dado de baja
     */
    @PostMapping("/{turnoId}/baja")
    @Transactional
    public ResponseEntity<TurnoResponseDto> darDeBaja(
            @PathVariable Long profesionalId,
            @PathVariable Long turnoId,
            @Valid @RequestBody DarDeBajaTurnoRequestDto request,
            @RequestHeader(value = "X-Usuario", defaultValue = "profesional") String usuario) {

        Turno turno = darDeBajaTurno.ejecutar(profesionalId, turnoId, request.getMotivo(), usuario);
        Turno turnoConRelaciones = turnoRepository.findByIdConRelaciones(turno.getId()).orElse(turno);
        TurnoResponseDto responseDto = TurnoResponseDto.from(turnoConRelaciones, PoliticaTransicionesTurno.DADO_DE_BAJA);
        return ResponseEntity.ok(responseDto);
    }
}
