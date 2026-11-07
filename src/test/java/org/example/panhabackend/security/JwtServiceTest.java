package org.example.panhabackend.security;

import io.jsonwebtoken.JwtException;
import org.example.panhabackend.config.AppProperties;
import org.example.panhabackend.entity.Role;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("unit-test-secret-key-that-is-long-enough-for-hs256!!".getBytes());

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt(SECRET, 15, 7, "test-issuer"),
                new AppProperties.Cors(List.of()),
                new AppProperties.Attendance(true, 7),
                new AppProperties.PasswordReset(30, "http://localhost/reset"),
                new AppProperties.Bootstrap(null, null, null));
        jwtService = new JwtService(props);

        User user = new User();
        user.setId(42L);
        user.setUsername("teacher1");
        user.setPassword("hash");
        user.setTokenVersion(3);
        user.setRoles(Set.of(new Role(RoleName.ROLE_TEACHER, null)));
        principal = new UserPrincipal(user);
    }

    @Test
    void accessTokenRoundTrips() {
        String token = jwtService.generateAccessToken(principal);

        JwtService.TokenClaims claims = jwtService.parse(token, JwtService.TokenType.ACCESS);

        assertThat(claims.username()).isEqualTo("teacher1");
        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.tokenVersion()).isEqualTo(3);
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(15 * 60);
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refresh = jwtService.generateRefreshToken(principal);

        assertThatThrownBy(() -> jwtService.parse(refresh, JwtService.TokenType.ACCESS))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(principal);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtService.parse(tampered, JwtService.TokenType.ACCESS))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRefused() {
        AppProperties weak = new AppProperties(
                new AppProperties.Jwt(Base64.getEncoder().encodeToString("short".getBytes()), 15, 7, "x"),
                null, null, null, null);

        assertThatThrownBy(() -> new JwtService(weak)).isInstanceOf(IllegalStateException.class);
    }
}
