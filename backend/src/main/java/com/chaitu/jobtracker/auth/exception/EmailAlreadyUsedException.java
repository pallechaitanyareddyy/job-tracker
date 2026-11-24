package com.chaitu.jobtracker.auth.exception;

import org.springframework.http.HttpStatus;

import com.chaitu.jobtracker.common.exception.ApiException;

/** Registering with an email that already has an account. Becomes HTTP 409 Conflict. */
public class EmailAlreadyUsedException extends ApiException {

    public EmailAlreadyUsedException() {
        super(HttpStatus.CONFLICT, "An account with this email already exists");
    }
}
