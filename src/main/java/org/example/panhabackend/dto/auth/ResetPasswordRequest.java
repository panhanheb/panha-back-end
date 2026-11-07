package org.example.panhabackend.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword,
        @NotBlank String confirmPassword) {

    @AssertTrue(message = "New password and confirmation do not match")
    public boolean isPasswordConfirmed() {
        return Objects.equals(newPassword, confirmPassword);
    }
}
