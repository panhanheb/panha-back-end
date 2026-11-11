package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.AuthResponse;
import com.example.ecommerce.dto.request.ChangePasswordRequest;
import com.example.ecommerce.dto.request.LoginRequest;
import com.example.ecommerce.dto.request.RegisterRequest;
import com.example.ecommerce.dto.request.ResetPasswordRequest;
import com.example.ecommerce.dto.response.UserResponse;

/**
 * Sign-up, sign-in and the token lifecycle. Credentials are checked by UserAccountService; this service
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
