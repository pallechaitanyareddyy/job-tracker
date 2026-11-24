package com.chaitu.jobtracker.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chaitu.jobtracker.auth.dto.LoginRequest;
import com.chaitu.jobtracker.auth.dto.RegisterRequest;
import com.chaitu.jobtracker.auth.exception.EmailAlreadyUsedException;
import com.chaitu.jobtracker.auth.exception.InvalidCredentialsException;
import com.chaitu.jobtracker.user.dto.UserResponse;
import com.chaitu.jobtracker.user.entity.User;
import com.chaitu.jobtracker.user.repository.UserRepository;

/**
 * Unit test of the business rules. Every collaborator is a Mockito fake,
 * so there is no database and no Spring: the test runs in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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
    void registerLowercasesTheEmailAndStoresOnlyTheHash() {
        when(userRepository.existsByEmail("chaitu@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!123")).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserResponse response =
                authService.register(new RegisterRequest("  Chaitu@Example.COM ", "Passw0rd!123", " Chaitu "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("chaitu@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASHED");
        assertThat(saved.getValue().getFullName()).isEqualTo("Chaitu");
        assertThat(response.email()).isEqualTo("chaitu@example.com");
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyUsed() {
        when(userRepository.existsByEmail("chaitu@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("chaitu@example.com", "Passw0rd!123", "Chaitu")))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerTurnsADatabaseUniqueViolationIntoAConflict() {
        when(userRepository.existsByEmail("chaitu@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!123")).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("chaitu@example.com", "Passw0rd!123", "Chaitu")))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void loginWithTheRightPasswordReturnsBothTokens() {
        User user = new User("chaitu@example.com", "HASH", "Chaitu");
        when(userRepository.findByEmail("chaitu@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd!123", "HASH")).thenReturn(true);
        when(jwtService.createAccessToken(any(), eq(List.of("USER")))).thenReturn("ACCESS");
        when(jwtService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(refreshTokenService.issue(any())).thenReturn("REFRESH");

        LoginResult result = authService.login(new LoginRequest("Chaitu@Example.com", "Passw0rd!123"));

        assertThat(result.accessToken()).isEqualTo("ACCESS");
        assertThat(result.refreshToken()).isEqualTo("REFRESH");
        assertThat(result.expiresInSeconds()).isEqualTo(900);
        assertThat(result.user().email()).isEqualTo("chaitu@example.com");
    }

    @Test
    void loginWithTheWrongPasswordIsRejectedAndIssuesNoTokens() {
        User user = new User("chaitu@example.com", "HASH", "Chaitu");
        when(userRepository.findByEmail("chaitu@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("chaitu@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).createAccessToken(any(), any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void loginWithAnUnknownEmailStillComparesAHashAndIsRejected() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(eq("whatever-password"), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "whatever-password")))
                .isInstanceOf(InvalidCredentialsException.class);

        // The hash comparison happened even though no such user exists (this protects against timing attacks).
        verify(passwordEncoder).matches(eq("whatever-password"), anyString());
        verifyNoInteractions(refreshTokenService);
    }
}
