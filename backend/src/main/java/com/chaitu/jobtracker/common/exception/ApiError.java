package com.chaitu.jobtracker.common.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

/** The one JSON shape every error response uses. */
public record ApiError(Instant timestamp, int status, String message, List<FieldIssue> fieldErrors) {

    /** One problem with one field of a request body, for example "password: size must be between 8 and 72". */
    public record FieldIssue(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String message) {
        return new ApiError(Instant.now(), status.value(), message, List.of());
    }
}
