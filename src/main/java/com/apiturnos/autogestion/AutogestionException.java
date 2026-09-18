package com.apiturnos.autogestion;

public class AutogestionException extends RuntimeException {
    private final int status;
    public AutogestionException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
