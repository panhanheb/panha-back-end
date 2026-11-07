package org.example.panhabackend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Uniform error body returned by every failing endpoint.
 *
 * @param fieldErrors validation errors keyed by field name (only for 400 validation failures)
 * @param details     extra information, such as the list of duplicate students in a bulk request
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(Instant timestamp,
                            int status,
                            String error,
                            String message,
                            String path,
                            Map<String, String> fieldErrors,
                            List<String> details) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, Map.of(), List.of());
    }
}
