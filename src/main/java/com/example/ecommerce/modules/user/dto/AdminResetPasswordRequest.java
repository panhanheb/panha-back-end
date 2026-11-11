package com.example.ecommerce.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.example.ecommerce.common.security.PasswordRules;

public record AdminResetPasswordRequest(
        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword) {
}
