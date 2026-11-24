package com.chaitu.jobtracker.auth.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaitu.jobtracker.auth.dto.LoginRequest;
import com.chaitu.jobtracker.auth.dto.LoginResponse;
import com.chaitu.jobtracker.auth.dto.RegisterRequest;
import com.chaitu.jobtracker.auth.security.SecurityProperties;
import com.chaitu.jobtracker.auth.service.AuthService;
import com.chaitu.jobtracker.auth.service.LoginResult;
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
        LoginResult result = authService.login(request);

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
}
