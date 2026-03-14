package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class GoogleBooksAuthException extends RuntimeException {
    private final HttpStatusCode status;

    public GoogleBooksAuthException(String message, HttpStatusCode status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}