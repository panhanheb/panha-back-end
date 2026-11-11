package com.example.ecommerce.common.security;

import java.util.Optional;

/**
 * Answers "is this account still allowed in, and which session version is current?" for every
 * authenticated request. Implemented by the user module; {@code common} only knows this interface.
 */
public interface SessionVersionLookup {

    /** The account's current session version, or empty when it is deleted or disabled. */
    Optional<Integer> activeSessionVersion(Long userId);
}
