package com.chaitu.jobtracker.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Base class for errors we throw on purpose. Each one carries the HTTP status it
 * should produce, so GlobalExceptionHandler needs only one rule for all of them
 * and the common package never has to know about the auth or user modules.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
