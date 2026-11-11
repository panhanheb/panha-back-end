package com.example.ecommerce.common.security;

import com.example.ecommerce.common.config.AppProperties;
import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and verifies access tokens: HMAC-signed JWTs (HS256/384/512, chosen from the key length) with the user id ({@code sub}), username, roles
 * and session version ({@code ver}). Refresh tokens are not JWTs; the auth module stores them hashed.
 */
@Component
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_VERSION = "ver";
    private static final String CLAIM_TYPE = "typ";
    private static final String ACCESS = "access";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtService(AppProperties appProperties, Clock clock) {
        AppProperties.Jwt jwt = appProperties.jwt();
        // Keys.hmacShaKeyFor rejects keys shorter than 256 bits, so a weak secret fails at startup
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
        this.issuer = jwt.issuer();
        this.accessTokenTtl = jwt.accessTokenTtl();
        this.clock = clock;
    }

    /** The verified content of an access token. */
    public record AccessClaims(Long userId, String username, List<RoleName> roles, int sessionVersion,
                               Instant expiresAt) {
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public IssuedToken issueAccessToken(Long userId, String username, Collection<RoleName> roles, int sessionVersion) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(accessTokenTtl);
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(userId.toString())
                .claim(CLAIM_TYPE, ACCESS)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles.stream().map(Enum::name).toList())
                .claim(CLAIM_VERSION, sessionVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * @throws BusinessException {@link ErrorCode#TOKEN_EXPIRED} or {@link ErrorCode#INVALID_TOKEN}
     */
    public AccessClaims parseAccessToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "The access token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "The access token is invalid");
        }
        try {
            if (!ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN, "Not an access token");
            }
            List<?> roles = claims.get(CLAIM_ROLES, List.class);
            return new AccessClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_USERNAME, String.class),
                    roles.stream().map(r -> RoleName.valueOf(r.toString())).toList(),
                    claims.get(CLAIM_VERSION, Integer.class),
                    claims.getExpiration().toInstant());
        } catch (RuntimeException ex) {
            if (ex instanceof BusinessException business) {
                throw business;
            }
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "The access token is invalid");
        }
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }
}
