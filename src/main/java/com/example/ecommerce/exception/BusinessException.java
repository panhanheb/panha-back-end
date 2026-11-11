package com.example.ecommerce.exception;

import java.util.List;

/**
 * Base class of the exceptions services throw for expected, client-caused failures. The global handler
 * turns them into an error response with the exception's {@link ErrorCode} and HTTP status.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final List<String> details;

    public BusinessException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public BusinessException(ErrorCode code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
