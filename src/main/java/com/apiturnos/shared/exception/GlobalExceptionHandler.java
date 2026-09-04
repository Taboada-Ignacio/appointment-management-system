package com.apiturnos.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.ConcurrencyFailureException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ExcepcionAgendaSuperpuestaException.class)
    public ResponseEntity<Map<String, Object>> handleExcepcionAgendaSuperpuesta(
            ExcepcionAgendaSuperpuestaException ex, HttpServletRequest request) {
        log.warn("Superposición de excepciones en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", HttpStatus.CONFLICT.value(),
                "error", HttpStatus.CONFLICT.getReasonPhrase(),
                "codigo", "EXCEPCION_AGENDA_SUPERPUESTA",
                "message", ex.getMessage(),
                "path", request.getRequestURI(),
                "coincidencias", ex.getCoincidencias()));
    }

    @ExceptionHandler(EntidadNoEncontradaException.class)
    public ResponseEntity<ErrorResponseDto> handleEntidadNoEncontrada(
            EntidadNoEncontradaException ex, HttpServletRequest request) {
        log.warn("Entidad no encontrada en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler({
            ClienteNoPerteneceProfesionalException.class,
            TipoAtencionNoPerteneceProfesionalException.class,
            TurnoNoPerteneceProfesionalException.class
    })
    public ResponseEntity<ErrorResponseDto> handleNoPerteneceProfesional(
            NegocioException ex, HttpServletRequest request) {
        log.warn("Acceso no autorizado en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler({
            ProfesionalDuplicadoException.class,
            ProfesionalConDependenciasException.class,
            ClienteDuplicadoException.class,
            AgendaAnualDuplicadaException.class,
            ConcurrencyFailureException.class,
            OptimisticLockException.class,
            PessimisticLockException.class
    })
    public ResponseEntity<ErrorResponseDto> handleConflictosYDuplicados(
            Exception ex, HttpServletRequest request) {
        log.warn("Conflicto/Duplicado en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler({
            DiaAgendaNoValidoException.class,
            CapacidadAgotadaException.class,
            EstadoInvalidoException.class,
            EstadoClienteInvalidoException.class,
            TransicionEstadoInvalidaException.class,
            ClienteNoPendienteDeVerificacionException.class,
            ClienteNoDadoDeBajaException.class,
            NegocioException.class
    })
    public ResponseEntity<ErrorResponseDto> handleNegocioException(
            NegocioException ex, HttpServletRequest request) {
        log.warn("NegocioException en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Error de validación en {}: {}", request.getRequestURI(), errors);

        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                "Error de Validación",
                "Uno o más campos contienen valores inválidos",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo JSON inválido en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cuerpo de la solicitud no contiene un JSON válido",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("IllegalArgumentException en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled Exception en {}: ", request.getRequestURI(), ex);
        ErrorResponseDto error = new ErrorResponseDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
