package com.example.ecommerce.service;

/**
 * Delivers password reset links. The default implementation only logs the link; replace it with an
 * e-mail sender (Phase 12, notifications) before going live.
 */
public interface PasswordResetNotifier {

    void sendResetLink(String email, String username, String resetLink, long expiresInMinutes);
}
