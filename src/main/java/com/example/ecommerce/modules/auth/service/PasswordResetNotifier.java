package com.example.ecommerce.modules.auth.service;

import com.example.ecommerce.modules.user.entity.User;

/**
 * Delivers password reset links. The default implementation logs the link; replace it with an
 * e-mail / SMS implementation (for example using spring-boot-starter-mail) in production.
 */
public interface PasswordResetNotifier {

    void sendResetLink(User user, String resetLink, long expiresInMinutes);
}
