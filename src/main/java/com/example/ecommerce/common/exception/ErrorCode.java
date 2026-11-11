package com.example.ecommerce.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes returned in {@code error.code}. Clients (Flutter, web, admin) branch on
 * these, never on message text. Add codes; never rename or remove one that has shipped.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED),
    /** The access token has expired: call POST /auth/refresh, then retry. */
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    /** The token is malformed, forged, or its session was ended (logout-all, password change...). Sign in again. */
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    /** A refresh token was used twice; every session of that sign-in was ended. Sign in again. */
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    INVALID_ORDER_TRANSITION(HttpStatus.CONFLICT),
    PAYMENT_DECLINED(HttpStatus.UNPROCESSABLE_CONTENT),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
