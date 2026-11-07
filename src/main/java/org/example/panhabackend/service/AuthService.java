package org.example.panhabackend.service;

import org.example.panhabackend.dto.auth.AuthResponse;
import org.example.panhabackend.dto.auth.ChangePasswordRequest;
import org.example.panhabackend.dto.auth.ForgotPasswordRequest;
import org.example.panhabackend.dto.auth.LoginRequest;
import org.example.panhabackend.dto.auth.RefreshTokenRequest;
import org.example.panhabackend.dto.auth.RegisterRequest;
import org.example.panhabackend.dto.auth.ResetPasswordRequest;
import org.example.panhabackend.dto.user.UserResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout();

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    UserResponse currentUser();
}
