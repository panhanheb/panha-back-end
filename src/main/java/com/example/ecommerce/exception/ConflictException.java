package com.example.ecommerce.exception;

import java.util.List;

/** 409: the request conflicts with the current state (duplicate, out of stock, invalid status change...). */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(String message, List<String> details) {
        super(ErrorCode.CONFLICT, message, details);
    }

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }

    public ConflictException(ErrorCode code, String message, List<String> details) {
        super(code, message, details);
    }
}
