package com.chaitu.jobtracker.auth.exception;

import org.springframework.http.HttpStatus;

import com.chaitu.jobtracker.common.exception.ApiException;

/**
 * The refresh token is missing, unknown, expired, or was already used. Becomes HTTP 401.
 * One message for every case, so a caller learns nothing about which tokens exist.
 */
public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }
}
