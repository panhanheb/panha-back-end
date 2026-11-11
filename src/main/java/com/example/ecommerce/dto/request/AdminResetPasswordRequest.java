package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.example.ecommerce.util.PasswordRules;

public record AdminResetPasswordRequest(
        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword) {
}
