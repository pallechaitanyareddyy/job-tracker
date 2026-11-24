package com.chaitu.jobtracker.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.chaitu.jobtracker.user.entity.User;

/** What the API tells the outside world about a user. The password hash is deliberately not here. */
public record UserResponse(UUID id, String email, String fullName, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt());
    }
}
