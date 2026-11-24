package com.chaitu.jobtracker.auth.service;

import com.chaitu.jobtracker.user.dto.UserResponse;

/**
 * What AuthService hands back to the controller after a login.
 * It includes the raw refresh token, which the controller puts into a cookie and never into the JSON body.
 */
public record LoginResult(
        String accessToken,
        long expiresInSeconds,
        String refreshToken,
        UserResponse user) {
}
