package com.example.ecommerce.modules.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword,
        @NotBlank String confirmPassword) {

    @AssertTrue(message = "New password and confirmation do not match")
    public boolean isPasswordConfirmed() {
        return Objects.equals(newPassword, confirmPassword);
    }
}
