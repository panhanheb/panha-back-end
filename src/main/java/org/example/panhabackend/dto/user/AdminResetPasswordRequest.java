package org.example.panhabackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.example.panhabackend.dto.auth.PasswordRules;

public record AdminResetPasswordRequest(
        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword) {
}
