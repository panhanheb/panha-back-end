package com.example.ecommerce.modules.auth.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.security.SecurityUtils;
import com.example.ecommerce.modules.auth.dto.AuthResponse;
import com.example.ecommerce.modules.auth.dto.ChangePasswordRequest;
import com.example.ecommerce.modules.auth.dto.ForgotPasswordRequest;
import com.example.ecommerce.modules.auth.dto.LoginRequest;
import com.example.ecommerce.modules.auth.dto.RefreshTokenRequest;
import com.example.ecommerce.modules.auth.dto.RegisterRequest;
import com.example.ecommerce.modules.auth.dto.ResetPasswordRequest;
import com.example.ecommerce.modules.auth.service.AuthService;
import com.example.ecommerce.modules.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "1. Auth", description = "Sign-up, sign-in, tokens and password recovery")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Create a customer account",
            description = "Creates a customer and signs it in: the response contains the token pair. "
                    + "Rate limited per client IP.")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest http) {
        AuthResponse body = authService.register(request, client(http));
        return ResponseEntity.created(URI.create("/api/v1/auth/me"))
                .body(ApiResponse.ok(body, "Account created"));
    }

    @Operation(summary = "Sign in with username or e-mail",
            description = "Errors: INVALID_CREDENTIALS, ACCOUNT_DISABLED, RATE_LIMITED.")
    @SecurityRequirements
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.login(request, client(http)));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair",
            description = "The refresh token is single use: store the new one. Using an old one again ends the "
                    + "whole sign-in (REFRESH_TOKEN_REUSED). Call this when a request fails with TOKEN_EXPIRED.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        return ApiResponse.ok(authService.refresh(request.refreshToken(), client(http)));
    }

    @Operation(summary = "Sign out this device",
            description = "Revokes the refresh token (and its predecessors). The access token stays valid until "
                    + "it expires, so clients should also discard it.")
    @SecurityRequirements
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.message("Signed out");
    }

    @Operation(summary = "Sign out on every device", description = "Ends all sessions, including this one, immediately.")
    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll() {
        authService.logoutEverywhere(SecurityUtils.requirePrincipal().getId());
        return ApiResponse.message("Signed out on all devices");
    }

    @Operation(summary = "The signed-in user")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.me(SecurityUtils.requirePrincipal().getId()));
    }

    @Operation(summary = "Change the password",
            description = "Ends every other session and returns a fresh token pair for this device.")
    @PutMapping("/password")
    public ApiResponse<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                    HttpServletRequest http) {
        return ApiResponse.ok(authService.changePassword(SecurityUtils.requirePrincipal().getId(), request,
                client(http)), "Password changed");
    }

    @Operation(summary = "Request a password reset link",
            description = "Always answers 202, whether or not the address is registered, so accounts cannot be "
                    + "discovered. The link is valid for 30 minutes.")
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.message("If an account uses this address, a reset link has been sent"));
    }

    @Operation(summary = "Set a new password with the token from the reset link",
            description = "Ends every session of the account; sign in again afterwards.")
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Password has been reset. Please sign in");
    }

    private static AuthService.ClientInfo client(HttpServletRequest http) {
        return new AuthService.ClientInfo(http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
    }
}
