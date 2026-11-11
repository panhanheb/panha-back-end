package com.example.ecommerce.dto.request;

import com.example.ecommerce.util.PasswordRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public customer sign-up. The new account always receives ROLE_CUSTOMER; administrators are promoted
 * afterwards on the Users page of the back office.
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, digits, '.', '_' and '-'")
        String username,

        @NotBlank @Email @Size(max = 120)
        String email,

        @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE)
        String password,

        @Size(max = 120)
        String fullName,

        @Size(max = 30)
        @Pattern(regexp = "^[+0-9 ()-]{6,30}$", message = "Phone may only contain digits, spaces, '+', '-', '(' and ')'")
        String phone) {
}
