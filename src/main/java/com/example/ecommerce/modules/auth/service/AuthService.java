package com.example.ecommerce.modules.auth.service;

import com.example.ecommerce.modules.auth.dto.AuthResponse;
import com.example.ecommerce.modules.auth.dto.ChangePasswordRequest;
import com.example.ecommerce.modules.auth.dto.LoginRequest;
import com.example.ecommerce.modules.auth.dto.RegisterRequest;
import com.example.ecommerce.modules.auth.dto.ResetPasswordRequest;
import com.example.ecommerce.modules.user.dto.UserResponse;

/**
 * Sign-up, sign-in and the token lifecycle. Credentials are checked by the user module; this module
 * issues access tokens (JWT) and manages refresh tokens.
 */
public interface AuthService {

    /** Who is calling: stored with refresh tokens to help users recognise their sessions. */
    record ClientInfo(String ipAddress, String userAgent) {
    }

    /** Creates a customer account and signs it in. */
    AuthResponse register(RegisterRequest request, ClientInfo client);

    AuthResponse login(LoginRequest request, ClientInfo client);

    /** Exchanges a refresh token for a new pair (rotation); a reused token ends the whole sign-in. */
    AuthResponse refresh(String refreshToken, ClientInfo client);

    /** Ends the sign-in the refresh token belongs to. Unknown tokens are ignored (idempotent). */
    void logout(String refreshToken);

    /** Ends every session of the current user on every device. */
    void logoutEverywhere(Long userId);

    /** Changes the password, ends every other session and returns a fresh pair for this device. */
    AuthResponse changePassword(Long userId, ChangePasswordRequest request, ClientInfo client);

    /** Sends a reset link when the address belongs to an active account; never reveals whether it does. */
    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    UserResponse me(Long userId);
}
