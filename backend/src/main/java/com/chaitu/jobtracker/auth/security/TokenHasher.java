package com.chaitu.jobtracker.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes refresh tokens with SHA-256 before they are stored.
 * A refresh token is 32 random bytes, so a fast hash is enough here;
 * BCrypt is only needed for passwords that humans choose.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    /** Returns 64 lowercase hex characters. */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Every Java runtime must provide SHA-256, so this cannot really happen.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
