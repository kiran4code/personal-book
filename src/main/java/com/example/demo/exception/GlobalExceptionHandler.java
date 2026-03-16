package com.example.demo.exception;

import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", ex.getMessage()));
    }
    @ExceptionHandler(GoogleBooksAuthException.class)
    public ResponseEntity<ApiError> handleGoogleAuth(GoogleBooksAuthException ex) {
        // returns 401 or 403 as thrown by service
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError("GOOGLE_BOOKS_AUTH_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(GoogleBooksClientException.class)
    public ResponseEntity<ApiError> handleGoogleUpstream(GoogleBooksClientException ex) {
        // returns 500/502/503/504 etc as thrown by service
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError("GOOGLE_BOOKS_UPSTREAM_ERROR", ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(500).body(new ApiError("INTERNAL_ERROR", "Unexpected error"));
    }

    @Value
    public static class ApiError {
        String code;
        String message;
    }
}