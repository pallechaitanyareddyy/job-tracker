package com.chaitu.jobtracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/auth/register.
 * A record is a short way to write an immutable class: Java generates the constructor,
 * the getters (email(), password(), ...), equals, hashCode and toString for us.
 * The password limit is 72 because BCrypt ignores everything after 72 bytes.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 150) String fullName) {
}
