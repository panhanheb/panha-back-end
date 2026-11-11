package com.example.ecommerce.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "Username or e-mail address", example = "admin")
        @NotBlank(message = "Username or email is required") @Size(max = 120)
        String login,

        @Schema(example = "Admin@123")
        @NotBlank(message = "Password is required") @Size(max = 100)
        String password) {
}
