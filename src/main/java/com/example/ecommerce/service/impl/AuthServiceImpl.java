package com.example.ecommerce.service.impl;

import com.example.ecommerce.config.AppProperties;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ErrorCode;
import com.example.ecommerce.security.JwtService;
import com.example.ecommerce.model.enums.AuditAction;
import com.example.ecommerce.service.AuditService;
import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.request.ResetPasswordRequest;
import com.example.ecommerce.model.RefreshToken;
import com.example.ecommerce.repository.RefreshTokenRepository;
import com.example.ecommerce.service.AuthService;
import com.example.ecommerce.service.PasswordResetNotifier;
import com.example.ecommerce.dto.response.AccountView;
import com.example.ecommerce.dto.response.UserResponse;
import com.example.ecommerce.service.UserAccountService;
import com.example.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ENTITY = "User";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserAccountService userAccountService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordResetNotifier passwordResetNotifier;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final Clock clock;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, ClientInfo client) {
        AccountView account = userAccountService.registerCustomer(request.username(), request.email(),
                request.password(), request.fullName(), request.phone());
        return issueTokens(account, UUID.randomUUID(), client);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse login(LoginRequest request, ClientInfo client) {
        AccountView account = userAccountService.authenticate(request.login(), request.password());
        return issueTokens(account, UUID.randomUUID(), client);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse refresh(String refreshToken, ClientInfo client) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(sha256(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token"));

        if (token.isRevoked()) {
            if (token.getReplacedById() != null) {
                // An already-rotated token came back: someone else holds a copy. End the whole sign-in.
                refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
                auditService.log(AuditAction.LOGOUT, ENTITY, token.getUserId(),
                        "Refresh token reuse detected; sign-in ended", token.getUserId(), null);
                log.warn("Refresh token reuse for user {} (family {}) from {}", token.getUserId(),
                        token.getFamilyId(), client.ipAddress());
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED,
                        "This refresh token was already used. Please sign in again");
            }
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "This session has ended. Please sign in again");
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "The refresh token has expired. Please sign in again");
        }

        AccountView account = userAccountService.getActiveAccount(token.getUserId());
        if (account.sessionVersion() != token.getSessionVersion()) {
            token.setRevokedAt(now);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "This session has ended. Please sign in again");
        }

        token.setRevokedAt(now);
        IssuedRefreshToken next = createRefreshToken(account, token.getFamilyId(), client);
        token.setReplacedById(next.entity().getId());
        return response(account, next);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHashForUpdate(sha256(refreshToken)).ifPresent(token -> {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant());
            auditService.log(AuditAction.LOGOUT, ENTITY, token.getUserId(), "Signed out", token.getUserId(), null);
        });
    }

    @Override
    @Transactional
    public void logoutEverywhere(Long userId) {
        userAccountService.endAllSessions(userId); // invalidates every access token at once
        refreshTokenRepository.revokeAllOfUser(userId, clock.instant());
    }

    @Override
    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request, ClientInfo client) {
        AccountView account = userAccountService.changePassword(userId, request.currentPassword(), request.newPassword());
        refreshTokenRepository.revokeAllOfUser(userId, clock.instant());
        return issueTokens(account, UUID.randomUUID(), client);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String token = randomToken();
        long ttlMinutes = appProperties.passwordReset().tokenExpirationMinutes();
        userAccountService.storePasswordResetToken(email, sha256(token), LocalDateTime.now().plusMinutes(ttlMinutes))
                .ifPresent(account -> passwordResetNotifier.sendResetLink(account.email(), account.username(),
                        appProperties.passwordReset().resetUrl() + "?token=" + token, ttlMinutes));
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(ResetPasswordRequest request) {
        AccountView account = userAccountService.resetPassword(sha256(request.token().trim()), request.newPassword());
        refreshTokenRepository.revokeAllOfUser(account.id(), clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userService.getById(userId);
    }

    /** Expired refresh tokens are useless; keep them one more day for investigating reuse reports. */
    @Scheduled(cron = "${app.jwt.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void deleteExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }

    // ------------------------------------------------------------------ helpers

    private record IssuedRefreshToken(String value, RefreshToken entity) {
    }

    private AuthResponse issueTokens(AccountView account, UUID familyId, ClientInfo client) {
        return response(account, createRefreshToken(account, familyId, client));
    }

    private AuthResponse response(AccountView account, IssuedRefreshToken refresh) {
        JwtService.IssuedToken access = jwtService.issueAccessToken(account.id(), account.username(),
                account.roles(), account.sessionVersion());
        return new AuthResponse("Bearer", access.value(), access.expiresAt(), refresh.value(),
                refresh.entity().getExpiresAt(), userService.getById(account.id()));
    }

    private IssuedRefreshToken createRefreshToken(AccountView account, UUID familyId, ClientInfo client) {
        String value = randomToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(account.id());
        token.setTokenHash(sha256(value));
        token.setFamilyId(familyId);
        token.setSessionVersion(account.sessionVersion());
        token.setExpiresAt(clock.instant().plus(appProperties.jwt().refreshTokenTtl()));
        token.setCreatedIp(truncate(client.ipAddress(), 45));
        token.setUserAgent(truncate(client.userAgent(), 255));
        refreshTokenRepository.saveAndFlush(token);
        return new IssuedRefreshToken(value, token);
    }

    /** 256 random bits, URL-safe. */
    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
