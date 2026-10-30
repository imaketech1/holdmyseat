package xyz.imaketech.holdmyseat.auth.token;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Only the SHA-256 hash of the token is stored. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private boolean revoked;

    protected RefreshToken() {
    }

    public RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() { return !revoked && expiresAt.isAfter(Instant.now()); }
    public void revoke() { this.revoked = true; }
    public UUID getUserId() { return userId; }
}
