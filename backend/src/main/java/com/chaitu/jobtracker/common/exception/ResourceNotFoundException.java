package com.chaitu.jobtracker.common.exception;

import org.springframework.http.HttpStatus;

/** The requested thing does not exist, or does not belong to the caller. Becomes HTTP 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
