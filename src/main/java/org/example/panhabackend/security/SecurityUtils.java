package org.example.panhabackend.security;

import org.example.panhabackend.enums.RoleName;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Static helpers to read the authenticated user from the security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal requirePrincipal() {
        return currentPrincipal().orElseThrow(() ->
                new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                        "No authenticated user"));
    }

    public static boolean hasRole(RoleName role) {
        return currentPrincipal().map(p -> p.hasRole(role)).orElse(false);
    }
}
