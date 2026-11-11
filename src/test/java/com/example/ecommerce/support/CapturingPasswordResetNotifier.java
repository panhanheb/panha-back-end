package com.example.ecommerce.support;

import com.example.ecommerce.service.PasswordResetNotifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps the last reset link per e-mail address so tests can follow it. */
public class CapturingPasswordResetNotifier implements PasswordResetNotifier {

    private final Map<String, String> lastLinkByEmail = new ConcurrentHashMap<>();

    @Override
    public void sendResetLink(String email, String username, String resetLink, long expiresInMinutes) {
        lastLinkByEmail.put(email, resetLink);
    }

    public String lastTokenFor(String email) {
        String link = lastLinkByEmail.get(email);
        return link == null ? null : link.substring(link.indexOf("token=") + "token=".length());
    }
}
