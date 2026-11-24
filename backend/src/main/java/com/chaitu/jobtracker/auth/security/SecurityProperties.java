package com.chaitu.jobtracker.auth.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the app.security.* settings in application.yml.
 * Spring maps jwt-secret to jwtSecret, access-token-ttl to accessTokenTtl, and so on.
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecret,
        String jwtIssuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean refreshCookieSecure) {
}
