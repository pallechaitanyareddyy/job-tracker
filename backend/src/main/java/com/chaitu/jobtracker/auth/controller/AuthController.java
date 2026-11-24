package com.chaitu.jobtracker.auth.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaitu.jobtracker.auth.dto.LoginRequest;
import com.chaitu.jobtracker.auth.dto.LoginResponse;
import com.chaitu.jobtracker.auth.dto.RegisterRequest;
import com.chaitu.jobtracker.auth.exception.InvalidRefreshTokenException;
import com.chaitu.jobtracker.auth.security.SecurityProperties;
import com.chaitu.jobtracker.auth.service.AuthService;
import com.chaitu.jobtracker.auth.service.LoginResult;
import com.chaitu.jobtracker.common.exception.ApiError;
import com.chaitu.jobtracker.user.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** HTTP layer only: reads the request, calls the service, shapes the response. No business rules here. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final SecurityProperties properties;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(authService.login(request));
    }

    /** The browser sends the refresh cookie by itself. The response carries a new access token and a new cookie. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        return tokenResponse(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedCookie().toString())
                .build();
    }

    /** A bad refresh token also deletes the cookie, so the browser stops sending a token that can never work. */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> invalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.SET_COOKIE, clearedCookie().toString())
                .body(ApiError.of(ex.getStatus(), ex.getMessage()));
    }

    private ResponseEntity<LoginResponse> tokenResponse(LoginResult result) {
        // HttpOnly: JavaScript cannot read it. SameSite=Strict: only sent on same-site requests.
        // Path: the browser sends it only to /api/v1/auth/..., never to ordinary API calls.
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(properties.refreshTokenTtl())
                .build();

        LoginResponse body = new LoginResponse(
                result.accessToken(), "Bearer", result.expiresInSeconds(), result.user());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    /** Same name, path and flags as the real cookie with Max-Age=0: that is how a server deletes a cookie. */
    private ResponseCookie clearedCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
