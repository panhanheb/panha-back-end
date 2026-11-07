package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.auth.AuthResponse;
import org.example.panhabackend.dto.auth.ChangePasswordRequest;
import org.example.panhabackend.dto.auth.ForgotPasswordRequest;
import org.example.panhabackend.dto.auth.LoginRequest;
import org.example.panhabackend.dto.auth.RefreshTokenRequest;
import org.example.panhabackend.dto.auth.RegisterRequest;
import org.example.panhabackend.dto.auth.ResetPasswordRequest;
import org.example.panhabackend.dto.common.MessageResponse;
import org.example.panhabackend.dto.user.UserResponse;
import org.example.panhabackend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Authentication", description = "Login, tokens, registration and password management")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Log in with username or email and receive JWT access + refresh tokens")
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Create a user account (ADMIN only), optionally linked to a teacher or student")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Exchange a refresh token for a new token pair")
    @SecurityRequirements
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "Log out: revokes every access and refresh token of the current user")
    @PostMapping("/logout")
    public MessageResponse logout() {
        authService.logout();
        return new MessageResponse("Logged out successfully");
    }

    @Operation(summary = "Change the current user's password (all existing tokens are revoked)")
    @PostMapping("/change-password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return new MessageResponse("Password changed successfully. Please log in again.");
    }

    @Operation(summary = "Request a password reset link (always returns the same message)")
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return new MessageResponse("If an account exists for that email, a password reset link has been sent.");
    }

    @Operation(summary = "Set a new password using the token from the reset link")
    @SecurityRequirements
    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Password has been reset. You can now log in with the new password.");
    }

    @Operation(summary = "Profile of the authenticated user")
    @GetMapping("/me")
    public UserResponse me() {
        return authService.currentUser();
    }
}
