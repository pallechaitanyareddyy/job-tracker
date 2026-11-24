package com.chaitu.jobtracker.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.chaitu.jobtracker.auth.security.SecurityProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Plain unit test: no Spring, no database. It builds the same encoder and
 * decoder the app uses.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-characters";
    private static final String OTHER_SECRET = "another-secret-that-is-also-at-least-32-chars";

    private final SecurityProperties properties = new SecurityProperties(SECRET, "test-issuer", Duration.ofMinutes(15),
            Duration.ofDays(7), false);

    private final SecretKey key = keyFrom(SECRET);

    private final JwtService jwtService = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties);

    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

    @Test
    void tokenCarriesUserIdIssuerAndRoles() {
        UUID userId = UUID.randomUUID();

        Jwt jwt = decoder.decode(jwtService.createAccessToken(userId, List.of("USER")));

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        // assertThat(jwt.getIssuer()).hasToString("test-issuer");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("test-issuer");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
    }

    @Test
    void tokenExpiresAfterTheConfiguredTime() {
        Jwt jwt = decoder.decode(jwtService.createAccessToken(UUID.randomUUID(), List.of("USER")));

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = jwtService.createAccessToken(UUID.randomUUID(), List.of("USER"));
        JwtDecoder decoderWithOtherKey = NimbusJwtDecoder.withSecretKey(keyFrom(OTHER_SECRET))
                .macAlgorithm(MacAlgorithm.HS256).build();

        assertThatThrownBy(() -> decoderWithOtherKey.decode(token)).isInstanceOf(JwtException.class);
    }

    private static SecretKey keyFrom(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
