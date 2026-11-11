package com.example.ecommerce.common.security;

import com.example.ecommerce.common.config.AppProperties;
import com.example.ecommerce.common.exception.BusinessException;
import com.example.ecommerce.common.exception.ErrorCode;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests of access-token issuing and verification (no Spring context). */
class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("unit-test-secret-that-is-long-enough-for-hs256-signing!!".getBytes());
    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");

    @Test
    void issuedTokenIsReadBackWithAllClaims() {
        JwtService jwt = service(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));
        JwtService.IssuedToken token = jwt.issueAccessToken(42L, "dara",
                List.of(RoleName.ROLE_CUSTOMER, RoleName.ROLE_SELLER), 3);

        JwtService.AccessClaims claims = jwt.parseAccessToken(token.value());

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.username()).isEqualTo("dara");
        assertThat(claims.roles()).containsExactly(RoleName.ROLE_CUSTOMER, RoleName.ROLE_SELLER);
        assertThat(claims.sessionVersion()).isEqualTo(3);
        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void expiredTokenIsReportedAsExpiredAfterTheClockSkew() {
        String token = service(SECRET, Clock.fixed(NOW, ZoneOffset.UTC))
                .issueAccessToken(1L, "u", List.of(RoleName.ROLE_CUSTOMER), 0).value();

        JwtService withinSkew = service(SECRET, Clock.fixed(NOW.plus(Duration.ofMinutes(15).plusSeconds(20)), ZoneOffset.UTC));
        assertThat(withinSkew.parseAccessToken(token).userId()).isEqualTo(1L);

        JwtService later = service(SECRET, Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.parseAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void tamperedOrForeignTokensAreInvalid() {
        JwtService jwt = service(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));
        String token = jwt.issueAccessToken(1L, "u", List.of(RoleName.ROLE_CUSTOMER), 0).value();

        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(Base64.getUrlDecoder().decode(parts[1])).replace("ROLE_CUSTOMER", "ROLE_ADMIN").getBytes());
        assertInvalid(jwt, parts[0] + "." + forgedPayload + "." + parts[2]);

        String otherSecret = Base64.getEncoder().encodeToString("another-secret-also-long-enough-for-hs256-signing!!!!".getBytes());
        String foreign = service(otherSecret, Clock.fixed(NOW, ZoneOffset.UTC))
                .issueAccessToken(1L, "u", List.of(RoleName.ROLE_ADMIN), 0).value();
        assertInvalid(jwt, foreign);

        assertInvalid(jwt, "not-a-jwt");
    }

    @Test
    void weakSecretsAreRejectedAtStartup() {
        String weak = Base64.getEncoder().encodeToString("short".getBytes());
        assertThatThrownBy(() -> service(weak, Clock.systemUTC())).isInstanceOf(WeakKeyException.class);
    }

    private static void assertInvalid(JwtService jwt, String token) {
        assertThatThrownBy(() -> jwt.parseAccessToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    private static JwtService service(String secret, Clock clock) {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt(secret, "test-issuer", Duration.ofMinutes(15), Duration.ofDays(30)),
                new AppProperties.RateLimit(false, Map.of()),
                null, null, null, null, null);
        return new JwtService(properties, clock);
    }
}
