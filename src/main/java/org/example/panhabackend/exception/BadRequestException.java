package org.example.panhabackend.exception;

/**
 * Thrown when a request is syntactically valid but violates a business rule. Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
