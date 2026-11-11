package com.example.ecommerce.exception;

import com.example.ecommerce.dto.response.ApiError;
import com.example.ecommerce.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns every exception into an {@link ApiResponse} failure with a stable {@link ErrorCode}. Internal
 * details (stack traces, SQL, class names) are logged, never returned.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        List<ApiError.Detail> details = ex.getDetails().stream().map(d -> new ApiError.Detail(null, d)).toList();
        return build(ex.getCode(), ex.getMessage(), details, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.Detail> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> details.add(new ApiError.Detail(fe.getField(), fe.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> details.add(new ApiError.Detail(null, ge.getDefaultMessage())));
        return build(ErrorCode.VALIDATION_FAILED, "Validation failed", details, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException ex,
                                                                    HttpServletRequest req) {
        List<ApiError.Detail> details = ex.getAllErrors().stream()
                .map(e -> new ApiError.Detail(null, e.getDefaultMessage())).toList();
        return build(ErrorCode.VALIDATION_FAILED, "Validation failed", details, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                       HttpServletRequest req) {
        List<ApiError.Detail> details = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.Detail(v.getPropertyPath().toString(), v.getMessage())).toList();
        return build(ErrorCode.VALIDATION_FAILED, "Validation failed", details, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpServletRequest req) {
        return build(ErrorCode.MALFORMED_REQUEST, "Malformed JSON request or invalid field value", List.of(), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpServletRequest req) {
        return build(ErrorCode.MALFORMED_REQUEST, "Send the request body as application/json", List.of(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest req) {
        return build(ErrorCode.BAD_REQUEST,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", List.of(), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                    HttpServletRequest req) {
        return build(ErrorCode.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'",
                List.of(), req);
    }

    /** Raised by Spring Data when {@code ?sort=} names an unknown property. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadSort(PropertyReferenceException ex, HttpServletRequest req) {
        return build(ErrorCode.BAD_REQUEST, "Invalid sort property '" + ex.getPropertyName() + "'", List.of(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(HttpServletRequest req) {
        return build(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password", List.of(), req);
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ApiResponse<Void>> handleDisabled(HttpServletRequest req) {
        return build(ErrorCode.ACCOUNT_DISABLED, "This account is disabled", List.of(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(HttpServletRequest req) {
        return build(ErrorCode.UNAUTHORIZED, "Authentication failed", List.of(), req);
    }

    /** From {@code @PreAuthorize}: the caller is signed in but lacks the role. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(HttpServletRequest req) {
        return build(ErrorCode.FORBIDDEN, "You do not have permission to perform this action", List.of(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex,
                                                          HttpServletRequest req) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(HttpServletRequest req) {
        return build(ErrorCode.NOT_FOUND, "Endpoint not found", List.of(), req);
    }

    /** Last line of defence for unique-key races (two requests creating the same SKU at once). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                 HttpServletRequest req) {
        log.warn("Data integrity violation on {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DUPLICATE_RESOURCE,
                "The operation conflicts with existing data (duplicate or referenced record)", List.of(), req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                                  HttpServletRequest req) {
        log.info("Concurrent modification on {}: {}", req.getRequestURI(), ex.getMessage());
        return build(ErrorCode.CONCURRENT_MODIFICATION,
                "The record was changed by another request; reload it and try again", List.of(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", List.of(), req);
    }

    /** Also used by the security entry points, which run before any controller. */
    public static ApiError apiError(ErrorCode code, String message, List<ApiError.Detail> details, String path) {
        return new ApiError(code.name(), message, details, path, MDC.get("requestId"));
    }

    private static ResponseEntity<ApiResponse<Void>> build(ErrorCode code, String message,
                                                           List<ApiError.Detail> details, HttpServletRequest req) {
        return ResponseEntity.status(code.status())
                .body(ApiResponse.failure(apiError(code, message, details, req.getRequestURI())));
    }
}
