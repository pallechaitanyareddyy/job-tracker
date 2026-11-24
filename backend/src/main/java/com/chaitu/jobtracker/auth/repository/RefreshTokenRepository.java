package com.chaitu.jobtracker.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chaitu.jobtracker.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes the token only if it is still active, in ONE database statement.
     * Returns 1 if this call revoked it, 0 if it was unknown, expired or already revoked.
     * Because the check and the change happen together, two requests that send the same token
     * at the same moment cannot both succeed: the database lets exactly one of them win.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken t set t.revokedAt = :now
            where t.tokenHash = :tokenHash and t.revokedAt is null and t.expiresAt > :now
            """)
    int revokeIfActive(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /** Ends every active session of one user. Used when a token is reused, which means it may have been stolen. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
