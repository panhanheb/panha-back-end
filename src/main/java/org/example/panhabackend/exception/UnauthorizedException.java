package org.example.panhabackend.exception;

/**
 * Thrown when a supplied credential (refresh token, current password) is invalid. Mapped to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
