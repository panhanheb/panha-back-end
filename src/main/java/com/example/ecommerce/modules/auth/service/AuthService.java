package com.example.ecommerce.modules.auth.service;

import com.example.ecommerce.modules.auth.dto.ChangePasswordRequest;
import com.example.ecommerce.modules.auth.dto.ForgotPasswordRequest;
import com.example.ecommerce.modules.auth.dto.RegisterRequest;
import com.example.ecommerce.modules.auth.dto.ResetPasswordRequest;
import com.example.ecommerce.modules.user.dto.UserResponse;

/**
 * Account self-service. Signing in and out is handled by Spring Security's form login; this service
 * only records those events.
 */
public interface AuthService {

    /** Public customer sign-up. */
    UserResponse register(RegisterRequest request);

    /** Called after a successful form login: stamps last-login time and writes the audit entry. */
    void recordLogin(Long userId);

    /** Called when a user signs out. */
    void recordLogout(Long userId, String username);

    /** Changes the current user's password and ends all of their sessions. */
    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserResponse currentUser();
}
