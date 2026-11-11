package com.example.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * The envelope of every JSON response.
 * <pre>
 * success: { "success": true,  "data": {...}, "message": "optional", "timestamp": "..." }
 * failure: { "success": false, "error": { "code": "...", "message": "...", ... }, "timestamp": "..." }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    /** For actions that return nothing but a confirmation, such as logout. */
    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message, null, Instant.now());
    }

    public static ApiResponse<Void> failure(ApiError error) {
        return new ApiResponse<>(false, null, null, error, Instant.now());
    }
}
