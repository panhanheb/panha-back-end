package com.example.ecommerce.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Error part of {@link ApiResponse}. {@code code} is stable and meant for clients to branch on
 * (see {@link com.example.ecommerce.common.exception.ErrorCode}); {@code message} is for people.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<Detail> details, String path, String traceId) {

    /** One problem with the request; {@code field} is set for validation errors, e.g. {@code items[0].quantity}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(String field, String message) {
    }
}
