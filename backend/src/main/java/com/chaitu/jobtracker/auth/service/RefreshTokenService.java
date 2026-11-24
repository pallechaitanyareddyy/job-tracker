package com.chaitu.jobtracker.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaitu.jobtracker.auth.entity.RefreshToken;
import com.chaitu.jobtracker.auth.repository.RefreshTokenRepository;
import com.chaitu.jobtracker.auth.security.SecurityProperties;
import com.chaitu.jobtracker.auth.security.TokenHasher;

import lombok.RequiredArgsConstructor;

/** Creates refresh tokens. Rotation and logout are added in slice 2b. */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final SecurityProperties properties;

    /**
     * Creates a new refresh token for the user and stores its hash.
     * Returns the raw token, which exists nowhere else: it goes to the browser and is then forgotten.
     */
    @Transactional
    public String issue(UUID userId) {
        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());
        repository.save(new RefreshToken(userId, TokenHasher.sha256Hex(rawToken), expiresAt));

        return rawToken;
    }
}
