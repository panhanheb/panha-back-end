package com.example.ecommerce.security;

import java.util.Optional;

/**
 * Answers "is this account still allowed in, and which session version is current?" for every
 * authenticated request. Implemented by UserAccountServiceImpl; the security layer only knows this interface.
 */
public interface SessionVersionLookup {

    /** The account's current session version, or empty when it is deleted or disabled. */
    Optional<Integer> activeSessionVersion(Long userId);
}
