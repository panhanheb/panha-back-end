package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of refresh and logout: the refresh token received at sign-in or the last refresh. */
public record RefreshTokenRequest(@NotBlank(message = "Refresh token is required") @Size(max = 200) String refreshToken) {
}
