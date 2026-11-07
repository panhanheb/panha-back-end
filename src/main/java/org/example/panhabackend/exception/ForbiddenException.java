package org.example.panhabackend.exception;

/**
 * Thrown when an authenticated user tries to access data they do not own or are not assigned to
 * (for example, a teacher recording attendance for a class they do not teach). Mapped to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
