package com.apiturnos.shared.exception;

public class NegocioException extends RuntimeException {
    public NegocioException(String message) {
        super(message);
    }
    public NegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
