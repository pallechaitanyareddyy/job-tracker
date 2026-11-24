package com.chaitu.jobtracker.common.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns exceptions thrown anywhere in a controller or service into clean JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Our own deliberate errors: 401, 404, 409 and so on. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getStatus(), ex.getMessage()));
    }

    /** A request body that failed Bean Validation (@NotBlank, @Email, @Size ...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldIssue(error.getField(), error.getDefaultMessage()))
                .toList();

        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Validation failed", issues);
        return ResponseEntity.badRequest().body(body);
    }
}
