package com.chaitu.jobtracker.auth.dto;

import com.chaitu.jobtracker.user.dto.UserResponse;

/**
 * Body returned by a successful login. The refresh token is NOT here:
 * it travels in an HttpOnly cookie that JavaScript cannot read.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
