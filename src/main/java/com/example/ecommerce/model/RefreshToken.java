package com.example.ecommerce.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One issued refresh token. Only its SHA-256 hash is stored, so a database leak does not leak usable
 * tokens. Tokens of one sign-in share a {@code familyId}; each refresh revokes the used token and
 * points {@code replacedById} at its successor. A revoked token that has a successor and is used again
 * proves theft, and the whole family is revoked.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken extends BaseEntity {

    /** Plain id, not an entity reference: tokens are cleaned up independently of the User entity. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    /** Session version of the account when issued; a later logout-all or password change invalidates it. */
    @Column(name = "session_version", nullable = false)
    private int sessionVersion;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    @Column(name = "created_ip", length = 45)
    private String createdIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
