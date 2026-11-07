package org.example.panhabackend.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.panhabackend.enums.RoleName;

import java.util.Set;

/**
 * Admin-controlled account creation. Optionally links the account to an existing teacher or student profile.
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, digits, '.', '_' and '-'")
        String username,

        @NotBlank @Email @Size(max = 120)
        String email,

        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)
        String password,

        @NotEmpty(message = "At least one role is required")
        Set<RoleName> roles,

        Long teacherId,

        Long studentId) {

    @AssertTrue(message = "An account can be linked to a teacher or a student, not both")
    public boolean isSingleProfileLink() {
        return teacherId == null || studentId == null;
    }
}
