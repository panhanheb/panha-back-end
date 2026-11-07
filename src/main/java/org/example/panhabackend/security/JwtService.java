package org.example.panhabackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.panhabackend.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and validates HMAC-SHA signed JWTs.
 * <p>
 * Claims: {@code sub} = username, {@code uid} = user id, {@code roles}, {@code typ} = ACCESS|REFRESH and
 * {@code ver} = the user's token version (used for revocation on logout / password change).
 */
@Service
public class JwtService {

    public enum TokenType { ACCESS, REFRESH }

    /** Parsed, verified token content. */
    public record TokenClaims(String username, Long userId, int tokenVersion, TokenType type) {
    }

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_VERSION = "ver";
    private static final String CLAIM_USER_ID = "uid";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(AppProperties properties) {
        AppProperties.Jwt jwt = properties.jwt();
        byte[] keyBytes = Decoders.BASE64.decode(jwt.secret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be a Base64 key of at least 256 bits");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = jwt.issuer();
        this.accessTtl = Duration.ofMinutes(jwt.accessTokenExpirationMinutes());
        this.refreshTtl = Duration.ofDays(jwt.refreshTokenExpirationDays());
    }

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, TokenType.ACCESS, accessTtl);
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return buildToken(principal, TokenType.REFRESH, refreshTtl);
    }

    public long getAccessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /**
     * Verifies signature, expiry, issuer and token type.
     *
     * @throws JwtException when the token is invalid, expired or of the wrong type
     */
    public TokenClaims parse(String token, TokenType expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.name().equals(type)) {
            throw new JwtException("Unexpected token type");
        }
        String version = claims.get(CLAIM_VERSION, String.class);
        String userId = claims.get(CLAIM_USER_ID, String.class);
        if (version == null || userId == null || claims.getSubject() == null) {
            throw new JwtException("Token is missing required claims");
        }
        try {
            return new TokenClaims(claims.getSubject(), Long.valueOf(userId), Integer.parseInt(version), expectedType);
        } catch (NumberFormatException ex) {
            throw new JwtException("Token contains malformed claims", ex);
        }
    }

    private String buildToken(UserPrincipal principal, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        List<String> roles = principal.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(principal.getUsername())
                .claim(CLAIM_USER_ID, String.valueOf(principal.getId()))
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, type.name())
                // Stored as a string so the value round-trips identically through any JSON library.
                .claim(CLAIM_VERSION, String.valueOf(principal.getTokenVersion()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }
}
