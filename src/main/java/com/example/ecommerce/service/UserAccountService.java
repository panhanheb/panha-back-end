package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.AccountView;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Credentials and account state, used by AuthService. Passwords are hashed and checked here only;
 * no other service ever sees a password hash.
 */
public interface UserAccountService {

    /**
     * Creates a customer account.
     *
     * @throws com.example.ecommerce.exception.ConflictException DUPLICATE_RESOURCE for a taken username or e-mail
     */
    AccountView registerCustomer(String username, String email, String rawPassword, String fullName, String phone);

    /**
     * Checks a username-or-e-mail and password and records the sign-in.
     *
     * @throws com.example.ecommerce.exception.BusinessException INVALID_CREDENTIALS or ACCOUNT_DISABLED
     */
    AccountView authenticate(String login, String rawPassword);

    /**
     * @throws com.example.ecommerce.exception.BusinessException INVALID_TOKEN when the account is gone or disabled
     */
    AccountView getActiveAccount(Long userId);

    /**
     * Verifies the current password, stores the new one and ends every session of the account.
     *
     * @return the account with its new session version
     */
    AccountView changePassword(Long userId, String currentPassword, String newPassword);

    /** Ends every session of the account (logout everywhere). Returns the new session version. */
    int endAllSessions(Long userId);

    /**
     * Stores a password reset token hash for the enabled account with this e-mail.
     *
     * @return the account, or empty when no enabled account uses the address
     */
    Optional<AccountView> storePasswordResetToken(String email, String tokenHash, LocalDateTime expiresAt);

    /**
     * Sets a new password using a reset token hash and ends every session of the account.
     *
     * @throws com.example.ecommerce.exception.BadRequestException when the token is unknown or expired
     */
    AccountView resetPassword(String tokenHash, String newPassword);
}
