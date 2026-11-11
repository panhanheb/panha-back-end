package com.example.ecommerce.modules.auth.service.impl;

import com.example.ecommerce.modules.auth.service.PasswordResetNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Development stand-in: writes the reset link to the log instead of e-mailing it. */
@Slf4j
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    @Override
    public void sendResetLink(String email, String username, String resetLink, long expiresInMinutes) {
        log.info("Password reset requested for '{}' <{}>. Link (valid {} min): {}",
                username, email, expiresInMinutes, resetLink);
    }
}
