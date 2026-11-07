package org.example.panhabackend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank(message = "Refresh token is required") String refreshToken) {
}
