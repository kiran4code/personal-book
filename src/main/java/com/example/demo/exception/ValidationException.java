package com.example.demo.exception;

/**
 * Thrown when request input is invalid or when upstream (Google Books) data
 * is missing required fields and should not be persisted.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}