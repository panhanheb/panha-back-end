package org.example.panhabackend.dto.auth;

import org.example.panhabackend.dto.user.UserResponse;

/**
 * @param expiresIn access token lifetime in seconds
 */
public record AuthResponse(String accessToken,
                           String refreshToken,
                           String tokenType,
                           long expiresIn,
                           UserResponse user) {
}
