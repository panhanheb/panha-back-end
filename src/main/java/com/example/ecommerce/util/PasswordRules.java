package com.example.ecommerce.util;

/**
 * Central password policy used by every request that sets a password.
 */
public final class PasswordRules {

    /** At least 8 characters with an uppercase letter, a lowercase letter, a digit and a special character. */
    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$";

    public static final String MESSAGE =
            "Password must be 8-100 characters and contain an uppercase letter, a lowercase letter, a digit and a special character";

    private PasswordRules() {
    }
}
