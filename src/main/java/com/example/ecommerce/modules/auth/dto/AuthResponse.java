package com.example.ecommerce.modules.auth.dto;

import com.example.ecommerce.modules.user.dto.UserResponse;

import java.time.Instant;

/**
 * Returned by sign-up, sign-in, refresh and password change.
 *
 * @param accessToken  send as {@code Authorization: Bearer <accessToken>}; short-lived
 * @param refreshToken store securely (Keychain / Keystore / secure storage); single use, replaced on refresh
 */
public record AuthResponse(String tokenType,
                           String accessToken,
                           Instant accessTokenExpiresAt,
                           String refreshToken,
                           Instant refreshTokenExpiresAt,
                           UserResponse user) {
}
