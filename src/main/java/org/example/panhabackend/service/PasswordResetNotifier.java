package org.example.panhabackend.service;

import org.example.panhabackend.entity.User;

/**
 * Delivers password reset links. The default implementation logs the link; replace it with an
 * e-mail / SMS implementation (for example using spring-boot-starter-mail) in production.
 */
public interface PasswordResetNotifier {

    void sendResetLink(User user, String resetLink, long expiresInMinutes);
}
