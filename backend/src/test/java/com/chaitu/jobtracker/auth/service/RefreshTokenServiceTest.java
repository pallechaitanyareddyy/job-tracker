package com.chaitu.jobtracker.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chaitu.jobtracker.auth.entity.RefreshToken;
import com.chaitu.jobtracker.auth.exception.InvalidRefreshTokenException;
import com.chaitu.jobtracker.auth.repository.RefreshTokenRepository;
import com.chaitu.jobtracker.auth.security.SecurityProperties;
import com.chaitu.jobtracker.auth.security.TokenHasher;

/** Unit test of the rotation rules with a fake repository: no database, no Spring. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String RAW = "raw-token-value";
    private static final String HASH = TokenHasher.sha256Hex(RAW);

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                "unit-test-secret-that-is-at-least-32-characters", "issuer",
                Duration.ofMinutes(15), Duration.ofDays(7), false);
        service = new RefreshTokenService(repository, properties);
    }

    private final UUID userId = UUID.randomUUID();

    @Test
    void rotateRevokesTheOldTokenAndIssuesANewOne() {
        RefreshToken stored = new RefreshToken(userId, HASH, Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(HASH)).thenReturn(Optional.of(stored));
        when(repository.revokeIfActive(eq(HASH), any(Instant.class))).thenReturn(1);

        RefreshTokenService.Rotation rotation = service.rotate(RAW);

        assertThat(rotation.userId()).isEqualTo(userId);
        assertThat(rotation.newRawToken()).isNotBlank().isNotEqualTo(RAW);
        verify(repository).save(any(RefreshToken.class));
        verify(repository, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void rotateRejectsAnUnknownToken() {
        when(repository.findByTokenHash(HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).save(any());
        verify(repository, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void reusingAnAlreadyRevokedTokenEndsAllSessionsOfThatUser() {
        RefreshToken stored = new RefreshToken(userId, HASH, Instant.now().plusSeconds(60));
        org.springframework.test.util.ReflectionTestUtils.setField(stored, "revokedAt", Instant.now().minusSeconds(5));
        when(repository.findByTokenHash(HASH)).thenReturn(Optional.of(stored));
        when(repository.revokeIfActive(eq(HASH), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository).revokeAllActiveForUser(eq(userId), any(Instant.class));
        verify(repository, never()).save(any());
    }

    @Test
    void anExpiredTokenIsRejectedWithoutEndingOtherSessions() {
        RefreshToken stored = new RefreshToken(userId, HASH, Instant.now().minusSeconds(60));
        when(repository.findByTokenHash(HASH)).thenReturn(Optional.of(stored));
        when(repository.revokeIfActive(eq(HASH), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        verify(repository, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void revokeOnlyTouchesTheHashOfTheGivenToken() {
        service.revoke(RAW);

        verify(repository).revokeIfActive(eq(HASH), any(Instant.class));
    }
}
