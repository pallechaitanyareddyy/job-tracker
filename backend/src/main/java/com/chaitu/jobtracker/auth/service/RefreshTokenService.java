package com.chaitu.jobtracker.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaitu.jobtracker.auth.entity.RefreshToken;
import com.chaitu.jobtracker.auth.exception.InvalidRefreshTokenException;
import com.chaitu.jobtracker.auth.repository.RefreshTokenRepository;
import com.chaitu.jobtracker.auth.security.SecurityProperties;
import com.chaitu.jobtracker.auth.security.TokenHasher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Creates, rotates and revokes refresh tokens. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final SecurityProperties properties;

    /** The user a refresh token belonged to, plus the brand-new token that replaces it. */
    public record Rotation(UUID userId, String newRawToken) {
    }

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

    /**
     * Swaps a refresh token for a new one. Each token works exactly once.
     *
     * If an already-used token shows up again, either the thief or the real owner is replaying it,
     * and we cannot tell which. So every session of that user is ended and they must log in again.
     *
     * noRollbackFor matters: normally a thrown exception undoes the whole transaction, which would also
     * undo "revoke all sessions". With it, the revocation is saved even though we still throw the 401.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String rawToken) {
        String hash = TokenHasher.sha256Hex(rawToken);
        Instant now = Instant.now();

        RefreshToken token = repository.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);

        if (repository.revokeIfActive(hash, now) == 1) {
            return new Rotation(token.getUserId(), issue(token.getUserId()));
        }

        if (token.getRevokedAt() != null) {
            int ended = repository.revokeAllActiveForUser(token.getUserId(), now);
            log.warn("Refresh token reuse detected for user {}: ended {} active session(s)", token.getUserId(), ended);
        }
        // Expired tokens end up here too, without the revoke-all.
        throw new InvalidRefreshTokenException();
    }

    /** Logout. Safe to call twice, or with an unknown token: it just does nothing then. */
    @Transactional
    public void revoke(String rawToken) {
        repository.revokeIfActive(TokenHasher.sha256Hex(rawToken), Instant.now());
    }
}
