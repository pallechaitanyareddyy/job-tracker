package com.chaitu.jobtracker.user.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chaitu.jobtracker.user.dto.UserResponse;
import com.chaitu.jobtracker.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * The first protected endpoint. The user id comes from the verified token (its subject),
     * never from the URL or the request body, so nobody can ask for someone else's profile.
     */
    @GetMapping("/profile")
    public UserResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(UUID.fromString(jwt.getSubject()));
    }
}
