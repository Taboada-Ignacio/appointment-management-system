package com.apiturnos.autogestion;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@Order(-1)
@RestControllerAdvice(basePackages = "com.apiturnos.autogestion")
public class AutogestionErrors {
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<?> missingHeader(org.springframework.web.bind.MissingRequestHeaderException e) {
        int status="Authorization".equals(e.getHeaderName())?401:400;
        return ResponseEntity.status(status).body(Map.of("status",status,"message","Falta la cabecera requerida: "+e.getHeaderName()));
    }
    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalidParameter(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("status",400,"message","Parámetros inválidos o incompletos"));
    }
    @ExceptionHandler(AutogestionException.class)
    public ResponseEntity<?> handle(AutogestionException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("status", e.getStatus(), "message", e.getMessage()));
    }
}
