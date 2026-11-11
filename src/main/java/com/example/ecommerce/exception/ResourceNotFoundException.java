package com.example.ecommerce.exception;

/** 404: the record does not exist, or the caller may not know it exists (another customer's order). */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.NOT_FOUND, resource + " not found with id: " + id);
    }
}
