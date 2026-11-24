package com.chaitu.jobtracker.auth.exception;

import org.springframework.http.HttpStatus;

import com.chaitu.jobtracker.common.exception.ApiException;

/**
 * Wrong email or wrong password. Becomes HTTP 401.
 * The message is the same for both cases on purpose, so an attacker cannot learn which emails have accounts.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
