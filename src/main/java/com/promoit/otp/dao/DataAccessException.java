package com.promoit.otp.dao;

/**
 * Unchecked wrapper around {@link java.sql.SQLException} so the service layer
 * is not forced to handle checked database exceptions everywhere.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
