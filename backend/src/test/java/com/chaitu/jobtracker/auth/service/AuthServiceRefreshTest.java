package com.chaitu.jobtracker.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chaitu.jobtracker.auth.exception.InvalidRefreshTokenException;
import com.chaitu.jobtracker.user.entity.User;
import com.chaitu.jobtracker.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void refreshReturnsANewAccessTokenAndANewRefreshToken() {
        UUID userId = UUID.randomUUID();
        User user = new User("chaitu@example.com", "HASH", "Chaitu");
        when(refreshTokenService.rotate("OLD")).thenReturn(new RefreshTokenService.Rotation(userId, "NEW"));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(any(), eq(List.of("USER")))).thenReturn("ACCESS");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));

        LoginResult result = authService.refresh("OLD");

        assertThat(result.accessToken()).isEqualTo("ACCESS");
        assertThat(result.refreshToken()).isEqualTo("NEW");
    }

    @Test
    void refreshWithoutACookieIsRejectedBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> authService.refresh(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh("  ")).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logoutRevokesTheTokenAndToleratesAMissingCookie() {
        authService.logout("OLD");
        authService.logout(null);

        verify(refreshTokenService).revoke("OLD");
        verifyNoMoreInteractions(refreshTokenService);
    }
}
