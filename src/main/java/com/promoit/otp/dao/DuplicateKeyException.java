package com.promoit.otp.dao;

/**
 * Raised when an insert violates a unique constraint (duplicate login or a
 * second administrator). The service layer maps this to an HTTP 409 conflict.
 */
public class DuplicateKeyException extends DataAccessException {

    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
