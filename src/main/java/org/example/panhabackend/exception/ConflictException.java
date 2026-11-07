package org.example.panhabackend.exception;

import java.util.List;

/**
 * Thrown on duplicates or state conflicts (for example, duplicate attendance or deleting a
 * referenced entity). Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    private final List<String> details;

    public ConflictException(String message) {
        this(message, List.of());
    }

    public ConflictException(String message, List<String> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<String> getDetails() {
        return details;
    }
}
