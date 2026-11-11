package com.example.ecommerce.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.ecommerce.common.security.PasswordRules;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword,
        @NotBlank String confirmPassword) {

    @JsonIgnore // validation only, not part of the JSON contract
    @AssertTrue(message = "New password and confirmation do not match")
    public boolean isPasswordConfirmed() {
        return Objects.equals(newPassword, confirmPassword);
    }
}
