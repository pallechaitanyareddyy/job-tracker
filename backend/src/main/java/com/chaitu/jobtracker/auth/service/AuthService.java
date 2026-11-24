package com.chaitu.jobtracker.auth.service;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chaitu.jobtracker.auth.dto.LoginRequest;
import com.chaitu.jobtracker.auth.dto.RegisterRequest;
import com.chaitu.jobtracker.auth.exception.EmailAlreadyUsedException;
import com.chaitu.jobtracker.auth.exception.InvalidCredentialsException;
import com.chaitu.jobtracker.auth.exception.InvalidRefreshTokenException;
import com.chaitu.jobtracker.user.dto.UserResponse;
import com.chaitu.jobtracker.user.entity.User;
import com.chaitu.jobtracker.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** The business rules of registering and logging in. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Every user has the USER role for now. An ADMIN role can be added later without changing the token format. */
    private static final List<String> DEFAULT_ROLES = List.of("USER");

    /**
     * A real BCrypt hash that matches no password. Login compares against it when the email is unknown,
     * so a wrong email takes as long as a wrong password and attackers cannot tell them apart by timing.
     */
    private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("dummy-password-for-timing");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /** The whole method is one transaction: either the user is saved, or nothing is. */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim());
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two people registered the same email at the same moment: the database constraint decides.
            throw new EmailAlreadyUsedException();
        }

        log.info("Registered user {}", user.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email).orElse(null);

        String hashToCheck = user != null ? user.getPasswordHash() : DUMMY_HASH;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches) {
            // The log never contains the email or the password.
            log.warn("Failed login attempt");
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.createAccessToken(user.getId(), DEFAULT_ROLES);
        String refreshToken = refreshTokenService.issue(user.getId());

        log.info("User {} logged in", user.getId());
        return new LoginResult(
                accessToken,
                jwtService.accessTokenTtl().toSeconds(),
                refreshToken,
                UserResponse.from(user));
    }

    /**
     * Trades a refresh token for a new access token and a new refresh token.
     * Deliberately NOT @Transactional: RefreshTokenService.rotate has its own transaction, and an outer one
     * would be marked rollback-only by the 401 and undo the "revoke all sessions" safety measure.
     */
    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);

        String accessToken = jwtService.createAccessToken(user.getId(), DEFAULT_ROLES);
        log.info("User {} refreshed their session", user.getId());
        return new LoginResult(
                accessToken,
                jwtService.accessTokenTtl().toSeconds(),
                rotation.newRawToken(),
                UserResponse.from(user));
    }

    /** Ends this one session. A missing cookie is fine: the user is effectively logged out already. */
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
