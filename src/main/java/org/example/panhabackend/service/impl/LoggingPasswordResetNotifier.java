package org.example.panhabackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.service.PasswordResetNotifier;
import org.springframework.stereotype.Component;

/**
 * Development notifier: writes the reset link to the application log instead of sending an e-mail.
 * Provide another {@link PasswordResetNotifier} bean (and remove this one) to deliver real e-mails.
 */
@Slf4j
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    @Override
    public void sendResetLink(User user, String resetLink, long expiresInMinutes) {
        log.info("Password reset requested for user '{}' <{}>. Link (valid {} min): {}",
                user.getUsername(), user.getEmail(), expiresInMinutes, resetLink);
    }
}
