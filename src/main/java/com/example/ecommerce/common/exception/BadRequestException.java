package com.example.ecommerce.common.exception;

/** 400: the request is understood but breaks a business rule (wrong current password, missing card number...). */
public class BadRequestException extends BusinessException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(ErrorCode code, String message) {
        super(code, message);
    }
}
